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
    private val store = FeSecureSessionStore(context.applicationContext)
    /**
     * Lives for the process lifetime together with this application-scoped singleton.
     * Jobs are bounded by API timeouts; SupervisorJob isolates one failed remote revoke.
     */
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** Owns every access/refresh credential and gate-state mutation. */
    private val sessionMutex = Mutex()

    private val _gateState = MutableStateFlow<FeAccountGateState>(FeAccountGateState.Checking)
    val gateState: StateFlow<FeAccountGateState> = _gateState.asStateFlow()

    private var accessToken: String? = null

    suspend fun checkGate() = validateOnline(showChecking = true)

    suspend fun revalidateOnline() = validateOnline(showChecking = false)

    private suspend fun validateOnline(showChecking: Boolean): Unit = serializedSessionOperation {
        if (showChecking) _gateState.value = FeAccountGateState.Checking
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
                    _gateState.value = FeAccountGateState.Blocked(decision.message, retryable = true)
                    return@serializedSessionOperation
                }
                is FeSoftwarePolicyDecision.ForceUpdate -> {
                    _gateState.value = FeAccountGateState.Blocked(
                        decision.message,
                        retryable = decision.updateUrl != null,
                        updateUrl = decision.updateUrl,
                    )
                    return@serializedSessionOperation
                }
            }

            val session = loadVerifiedSessionLocked()
            _gateState.value = if (session == null) {
                FeAccountGateState.LoginRequired
            } else {
                FeAccountGateState.Ready(session)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FeAccountNetworkException) {
            _gateState.value = FeAccountGateState.Blocked(
                message = "无法连接 LA Studio 服务，请检查网络后重试。",
                retryable = true,
            )
        } catch (e: FeAccountApiException) {
            applyGateApiErrorLocked(e.error)
        } catch (e: Exception) {
            _gateState.value = FeAccountGateState.Blocked(
                message = e.message ?: "登录状态检查失败",
                retryable = true,
            )
        }
    }

    suspend fun login(username: String, password: String, capToken: String? = null): Result<Unit> {
        return try {
            serializedSessionOperation {
                val tokens = api.login(username, password, capToken)
                store.writeRefreshToken(tokens.refreshToken)
                accessToken = tokens.accessToken
                try {
                    _gateState.value = FeAccountGateState.Ready(api.session(tokens.accessToken))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: FeAccountNetworkException) {
                    _gateState.value = FeAccountGateState.Blocked(
                        "登录已建立，但暂时无法验证会话。请检查网络后重试。",
                        retryable = true,
                    )
                    throw e
                } catch (e: FeAccountApiException) {
                    applyGateApiErrorLocked(e.error)
                    throw e
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
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
    private suspend fun loadVerifiedSessionLocked(): FeAccountSession? {
        val currentAccess = accessToken
        if (currentAccess != null) {
            try {
                return api.session(currentAccess)
            } catch (e: CancellationException) {
                throw e
            } catch (e: FeAccountApiException) {
                if (e.error.statusCode != 401) throw e
            }
        }

        val refreshToken = store.readRefreshToken() ?: run {
            accessToken = null
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
            api.session(rotated.accessToken)
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
                // A disconnect after request transmission has the same ambiguity as a timeout.
                clearCredentialsLocked()
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
    }

    companion object {
        private const val REFRESH_CRITICAL_TIMEOUT_MS = 10_000L
        @Volatile private var instance: FeAccountManager? = null

        fun get(context: Context): FeAccountManager = instance ?: synchronized(this) {
            instance ?: FeAccountManager(context).also { instance = it }
        }
    }
}
