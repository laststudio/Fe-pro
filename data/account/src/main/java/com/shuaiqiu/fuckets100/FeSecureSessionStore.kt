package com.shuaiqiu.fuckets100

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores only the rotating refresh credential; access credentials remain in memory. */
class FeSecureSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun readRefreshToken(): String? {
        val encoded = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            require(bytes.size > IV_BYTES)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES),
            )
            cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).toString(Charsets.UTF_8)
        }.getOrElse {
            clear()
            null
        }
    }

    @Synchronized
    fun writeRefreshToken(token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val value = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        check(prefs.edit().putString(KEY_REFRESH_TOKEN, value).commit()) {
            "无法安全保存登录凭据"
        }
    }

    @Synchronized
    fun clear() {
        check(prefs.edit().remove(KEY_REFRESH_TOKEN).commit()) {
            "无法清除本地登录凭据"
        }
    }

    @Synchronized
    fun readCachedSession(): FeAccountSession? {
        val raw = prefs.getString(KEY_CACHED_SESSION, null) ?: return null
        return runCatching {
            val json = JSONObject(raw)
            val user = json.getJSONObject("user")
            val level = user.optInt("member_level", 0)
            val tier = user.optString("tier").takeIf { it.isNotBlank() } ?: when (level) {
                2 -> "ultra"
                1 -> "pro"
                else -> "free"
            }
            FeAccountSession(
                sessionId = json.getString("session_id"),
                deviceId = json.optString("device_id").takeIf { it.isNotBlank() },
                user = FeAccountUser(
                    id = user.getLong("id"),
                    username = user.getString("username"),
                    memberLevel = level,
                    tier = tier,
                    avatarUrl = user.optString("avatar_url").takeIf { it.isNotBlank() },
                ),
                integrityCheckEnabled = json.optBoolean("integrity_check_enabled", true),
            )
        }.getOrNull()
    }

    @Synchronized
    fun writeCachedSession(session: FeAccountSession) {
        val user = JSONObject()
            .put("id", session.user.id)
            .put("username", session.user.username)
            .put("member_level", session.user.memberLevel)
            .put("tier", session.user.tier)
        session.user.avatarUrl?.let { user.put("avatar_url", it) }
        val json = JSONObject()
            .put("session_id", session.sessionId)
            .put("device_id", session.deviceId)
            .put("user", user)
            .put("integrity_check_enabled", session.integrityCheckEnabled)
        check(prefs.edit().putString(KEY_CACHED_SESSION, json.toString()).commit()) {
            "无法保存账号缓存"
        }
    }

    @Synchronized
    fun clearCachedSession() {
        prefs.edit().remove(KEY_CACHED_SESSION).commit()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    companion object {
        const val PREFS_NAME = "fe_account_secure"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_CACHED_SESSION = "cached_session"
        private const val KEY_ALIAS = "fe_account_refresh_v1"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BITS = 128
    }
}
