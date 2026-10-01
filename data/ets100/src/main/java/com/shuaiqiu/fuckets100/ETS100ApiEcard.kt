package com.shuaiqiu.fuckets100

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val TAG = "ETS100ApiClient"

    /**
     * 转义 JSON 字符串
     * 喵~ 用于安全地处理字符串喵！
     */
internal fun String.escapeJson(): String {
        return this
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

internal fun maskSensitiveToken(token: String): String {
        if (token.isBlank()) return "(empty)"
        if (token.length <= 12) return "${token.take(3)}***${token.takeLast(3)}"
        return "${token.take(6)}***${token.takeLast(6)}"
    }

internal fun parseEcardAccounts(bodyObj: org.json.JSONObject): List<ETS100ApiClient.EcardAccount> {
        val accounts = mutableListOf<ETS100ApiClient.EcardAccount>()
        val keys = bodyObj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val account = bodyObj.optJSONObject(key) ?: continue
            accounts += ETS100ApiClient.EcardAccount(
                key = key,
                id = account.optString("id"),
                parentId = account.optString("parent_id"),
                userAccountId = account.optString("user_account_id"),
                name = account.optString("name"),
                grade = account.optString("grade"),
                status = account.optString("status"),
                mobileStatus = account.optString("mobile_status"),
                outOfDate = account.optString("out_of_date"),
                classId = account.optString("class_id"),
                className = account.optString("class_name"),
                machineCodeStatus = account.optString("machine_code_status")
            )
        }
        return accounts
    }

internal fun selectEcardAccount(
        accounts: List<ETS100ApiClient.EcardAccount>,
        preferredAccountId: String?
    ): ETS100ApiClient.EcardAccount? {
        if (!preferredAccountId.isNullOrBlank()) {
            accounts.firstOrNull { account ->
                account.id == preferredAccountId && account.isValid
            }?.let { selected ->
                Log.i(
                    TAG,
                    "按保存/推荐 ecard_id 选择有效账户: key=${selected.key}, " +
                        "id=${selected.id}, parent_id=${selected.parentId}, name=${selected.name}"
                )
                return selected
            }
            Log.w(TAG, "preferredAccountId=$preferredAccountId 未匹配到有效账户")
        }

        accounts.firstOrNull { it.isValid }?.let { selected ->
            Log.i(
                TAG,
                "按有效班级选择账户: key=${selected.key}, " +
                    "id=${selected.id}, parent_id=${selected.parentId}, name=${selected.name}"
            )
            return selected
        }

        accounts.firstOrNull()?.let { selected ->
            Log.w(
                TAG,
                "未找到有效班级账户，回退第一个账户: key=${selected.key}, " +
                    "id=${selected.id}, parent_id=${selected.parentId}, name=${selected.name}"
            )
            return selected
        }

        return null
    }
