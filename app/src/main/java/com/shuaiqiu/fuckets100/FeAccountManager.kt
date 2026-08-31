package com.shuaiqiu.fuckets100

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class FeAccountManager private constructor(context: Context) {
    private val api = FeAccountApi(context.applicationContext)
    private val profileApi = FeAccountProfileApi()
    private val store = FeSecureSessionStore(context.applicationContext)
    /**
     * Lives for the process lifetime together with this application-scoped singleton.
     * Jobs are bounded by API timeouts; SupervisorJob isolates one failed remote revoke.
     */
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** Owns every access/refresh credential and gate-state mutation. */
    private val sessionMutex = Mutex()

    private var accessToken: String? = null
    private var verifiedSession: FeAccountSession? = store.readCachedSession()
    private var verifiedSessionAt = 0L
    private var cachedBindings: FeAccountProfileApi.AccountBindings? = null
    private var cachedBindingsAt = 0L
    private var cachedDevices: List<org.json.JSONObject>? = null
    private var cachedDevicesAt = 0L
    private var cachedSessions: List<org.json.JSONObject>? = null
    private var cachedSessionsAt = 0L

    private val _gateState = MutableStateFlow<FeAccountGateState>(
        FeAccountGateState.Checking(verifiedSession),
    )
    val gateState: StateFlow<FeAccountGateState> = _gateState.asStateFlow()

    suspend fun checkGate() = validateOnline(showChecking = true)

    suspend fun revalidateOnline() = validateOnline(showChecking = false)

    private suspend fun validateOnline(showChecking: Boolean): Unit = serializedSessionOperation {
        if (showChecking) _gateState.value = FeAccountGateState.Checking(verifiedSession)
        try {
            val policy = api.softwarePolicy()
            when (val decision = evaluateFeSoftwarePolicy(
                currentVersionCode = BuildConfig.VERSION_CODE,
                killSwitch = policy.killSwitch,
                latestVersionCode = policy.latestVersionCode,
                forceUpdate = policy.forceUpdate,
                message = policy.message,
                updateUrl = policy.updateUrl,
            )) {
                FeSoftwarePolicyDecision.Allowed -> Unit
                is FeSoftwarePolicyDecision.Blocked -> {
                    _gateState.value = FeAccountGateState.Blocked(
                        decision.message,
                        retryable = true,
                        cachedSession = verifiedSession,
                    )
                    return@serializedSessionOperation
                }
                is FeSoftwarePolicyDecision.ForceUpdate -> {
                    _gateState.value = FeAccountGateState.Blocked(
                        decision.message,
                        retryable = decision.updateUrl != null,
                        updateUrl = decision.updateUrl,
                        cachedSession = verifiedSession,
                    )
                    return@serializedSessionOperation
                }
            }

            val session = loadVerifiedSessionLocked(forceValidation = true)
            _gateState.value = if (session == null) {
                FeAccountGateState.LoginRequired
            } else {
                FeAccountGateState.Ready(session)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FeAccountNetworkException) {
            _gateState.value = FeAccountGateState.Offline(verifiedSession)
        } catch (e: FeAccountApiException) {
            applyGateApiErrorLocked(e.error)
        } catch (e: Exception) {
            _gateState.value = FeAccountGateState.Blocked(
                message = e.message ?: "登录状态检查失败",
                retryable = true,
                cachedSession = verifiedSession,
            )
        }
    }

    /**
     * 获取一个可用的 access token (必要时自动刷新)。
     * 未登录或会话失效时抛 [FeAccountNotAuthenticatedException]。
     */
    suspend fun requireAccessToken(): String = serializedSessionOperation {
        requireAccessTokenLocked()
    }

    private suspend fun requireAccessTokenLocked(): String {
        if (_gateState.value !is FeAccountGateState.Ready) {
            throw FeAccountNetworkException("LA Studio 尚未连接")
        }
        val session = loadVerifiedSessionLocked()
        if (session == null) {
            _gateState.value = FeAccountGateState.LoginRequired
            throw FeAccountNotAuthenticatedException()
        }
        return accessToken ?: throw FeAccountNotAuthenticatedException()
    }

    suspend fun login(username: String, password: String, capToken: String? = null): Result<Unit> {
        return try {
            serializedSessionOperation {
                val tokens = api.login(username, password, capToken)
                store.writeRefreshToken(tokens.refreshToken)
                accessToken = tokens.accessToken
                clearProfileCacheLocked()
                try {
                    val session = api.session(tokens.accessToken)
                    verifiedSession = session
                    verifiedSessionAt = now()
                    store.writeCachedSession(session)
                    _gateState.value = FeAccountGateState.Ready(session)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: FeAccountNetworkException) {
                    _gateState.value = FeAccountGateState.Offline(verifiedSession)
                    throw e
                } catch (e: FeAccountApiException) {
                    applyGateApiErrorLocked(e.error)
                    throw e
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FeAccountNetworkException) {
            _gateState.value = FeAccountGateState.Offline(verifiedSession)
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bindEts100Account(phone: String, password: String, deviceCode: String): Result<Unit> {
        return try {
            serializedSessionOperation {
                val token = requireAccessTokenLocked()
                api.bindEts100Account(token, phone, password, deviceCode)
                clearProfileCacheLocked()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal suspend fun cloudDeviceCode(): String = serializedSessionOperation {
        api.cloudDeviceCode(requireAccessTokenLocked())
    }

    internal suspend fun cloudSession(preferredAccountId: String): FeAccountApi.CloudSession =
        serializedSessionOperation {
            api.cloudSession(requireAccessTokenLocked(), preferredAccountId)
        }

    internal suspend fun accountBindings(force: Boolean = false): FeAccountProfileApi.AccountBindings =
        serializedSessionOperation {
            val cached = cachedBindings
            if (!force && cached != null && now() - cachedBindingsAt < PROFILE_CACHE_TTL_MS) return@serializedSessionOperation cached
            profileApi.bindings(requireAccessTokenLocked()).also {
                cachedBindings = it
                cachedBindingsAt = now()
            }
        }

    internal suspend fun accountDevices(force: Boolean = false): List<org.json.JSONObject> =
        serializedSessionOperation {
            val cached = cachedDevices
            if (!force && cached != null && now() - cachedDevicesAt < PROFILE_CACHE_TTL_MS) return@serializedSessionOperation cached
            profileApi.devices(requireAccessTokenLocked()).also {
                cachedDevices = it
                cachedDevicesAt = now()
            }
        }

    internal suspend fun accountSessions(force: Boolean = false): List<org.json.JSONObject> =
        serializedSessionOperation {
            val cached = cachedSessions
            if (!force && cached != null && now() - cachedSessionsAt < PROFILE_CACHE_TTL_MS) return@serializedSessionOperation cached
            profileApi.sessions(requireAccessTokenLocked()).also {
                cachedSessions = it
                cachedSessionsAt = now()
            }
        }

    private fun clearProfileCacheLocked() {
        cachedBindings = null
        cachedBindingsAt = 0L
        cachedDevices = null
        cachedDevicesAt = 0L
        cachedSessions = null
        cachedSessionsAt = 0L
    }

    internal suspend fun invalidateAccountData() = serializedSessionOperation {
        clearProfileCacheLocked()
    }

    suspend fun logout(onBeforeGateTransition: () -> Unit): Result<Unit> {
        // Disconnect an in-flight check/login so local logout does not wait for its network timeout.
        api.cancelActiveRequests()
        return try {
            withContext(NonCancellable) {
                sessionMutex.withLock {
                    val tokenToRevoke = accessToken
                    // clear() commits synchronously and may throw; do not navigate or change gate on failure.
                    clearCredentialsLocked()
                    try {
                        onBeforeGateTransition()
                    } finally {
                        _gateState.value = FeAccountGateState.LoginRequired
                    }
                    if (tokenToRevoke != null) scheduleRemoteLogout(tokenToRevoke)
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scheduleRemoteLogout(token: String) {
        managerScope.launch {
            try {
                api.logout(token)
            } catch (e: CancellationException) {
                throw e
            } catch (_: FeAccountApiException) {
                // Local logout is authoritative for this device; remote revoke is best effort.
            } catch (_: FeAccountNetworkException) {
                // The API has finite timeouts, so this process-owned job cannot leak indefinitely.
            }
        }
    }

    private suspend fun <T> serializedSessionOperation(block: suspend () -> T): T =
        sessionMutex.withLock { block() }

    /** Must be called with [sessionMutex] held. */
    private suspend fun loadVerifiedSessionLocked(forceValidation: Boolean = false): FeAccountSession? {
        val currentAccess = accessToken
        if (currentAccess != null) {
            if (!forceValidation && verifiedSession != null && now() - verifiedSessionAt < SESSION_CACHE_TTL_MS) {
                return verifiedSession
            }
            try {
                return api.session(currentAccess).also {
                    verifiedSession = it
                    verifiedSessionAt = now()
                    store.writeCachedSession(it)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: FeAccountApiException) {
                if (e.error.statusCode != 401) throw e
            }
        }

        val refreshToken = store.readRefreshToken() ?: run {
            accessToken = null
            verifiedSession = null
            return null
        }
        val rotated = try {
            rotateRefreshLocked(refreshToken)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FeAccountApiException) {
            if (e.error.statusCode == 401) clearCredentialsLocked()
            throw e
        }
        // The replacement token is durable; now restore the caller's deferred cancellation.
        currentCoroutineContext().ensureActive()
        return try {
            api.session(rotated.accessToken).also {
                verifiedSession = it
                verifiedSessionAt = now()
                store.writeCachedSession(it)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FeAccountApiException) {
            if (e.error.statusCode == 401) clearCredentialsLocked()
            throw e
        }
    }

    /**
     * Must be called with [sessionMutex] held. Caller cancellation is deferred until either the
     * bounded request fails or the replacement refresh token is synchronously committed.
     */
    private suspend fun rotateRefreshLocked(refreshToken: String): FeAccountApi.RefreshTokens =
        withContext(NonCancellable) {
            val rotated = try {
                withTimeoutOrNull(REFRESH_CRITICAL_TIMEOUT_MS) {
                    api.refresh(refreshToken)
                } ?: throw FeAccountNetworkException(
                    "刷新登录凭据结果不确定，请重新登录",
                )
            } catch (e: FeAccountNetworkException) {
                // Keep the refresh credential and cached profile for offline display. The next
                // online check can safely retry the refresh operation.
                throw e
            }
            // Once a response is received, never restore cancellation before this synchronous commit.
            store.writeRefreshToken(rotated.refreshToken)
            accessToken = rotated.accessToken
            rotated
        }

    /** Must be called with [sessionMutex] held. */
    private fun applyGateApiErrorLocked(error: FeApiError) {
        if (error.statusCode == 401) {
            clearCredentialsLocked()
            _gateState.value = FeAccountGateState.LoginRequired
            return
        }
        _gateState.value = FeAccountGateState.Blocked(
            message = error.message,
            retryable = isRetryableFeGateStatus(error.statusCode),
        )
    }

    /** Must be called with [sessionMutex] held. */
    private fun clearCredentialsLocked() {
        // Keep the in-memory credential intact if synchronous secure-store clearing fails.
        store.clear()
        accessToken = null
        verifiedSession = null
        verifiedSessionAt = 0L
        store.clearCachedSession()
        clearProfileCacheLocked()
    }

    private fun now(): Long = android.os.SystemClock.elapsedRealtime()

    companion object {
        private const val REFRESH_CRITICAL_TIMEOUT_MS = 10_000L
        private const val SESSION_CACHE_TTL_MS = 60_000L
        private const val PROFILE_CACHE_TTL_MS = 120_000L
        @Volatile private var instance: FeAccountManager? = null

        fun get(context: Context): FeAccountManager = instance ?: synchronized(this) {
            instance ?: FeAccountManager(context).also { instance = it }
        }
    }
}
