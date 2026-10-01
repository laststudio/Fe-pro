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

// ============================================================================
    // JSON 解析工具
// ============================================================================

    /**
     * 简化 JSON 字符串解析
     * 喵~ 支持 JSON 对象和数组喵！
     * 如果响应是数组（如 [{...}]），返回数组第一个元素作为 JSONObject
     */
internal fun String.parseJson(): org.json.JSONObject {
        val trimmed = this.trim()
        return if (trimmed.startsWith("[")) {
            // 响应是数组，取第一个元素
            val array = org.json.JSONArray(trimmed)
            if (array.length() > 0) {
                array.getJSONObject(0)
            } else {
                org.json.JSONObject()
            }
        } else {
            org.json.JSONObject(this)
        }
    }

internal fun extractBody(json: org.json.JSONObject): org.json.JSONObject {
        val bodyObj = json.optJSONObject("body")
        if (bodyObj != null) return bodyObj
        val bodyStr = json.optString("body", "")
        if (bodyStr.isNotBlank()) {
            try {
                val decodedBytes = android.util.Base64.decode(bodyStr, android.util.Base64.DEFAULT)
                val decodedStr = String(decodedBytes, Charsets.UTF_8).trim()
                if (decodedStr.startsWith("{")) {
                    return org.json.JSONObject(decodedStr)
                } else if (decodedStr.startsWith("[")) {
                    val arr = org.json.JSONArray(decodedStr)
                    val wrapper = org.json.JSONObject()
                    wrapper.put("data", arr)
                    return wrapper
                }
            } catch (_: Exception) {}
            try {
                val trimmed = bodyStr.trim()
                if (trimmed.startsWith("{")) {
                    return org.json.JSONObject(trimmed)
                } else if (trimmed.startsWith("[")) {
                    val arr = org.json.JSONArray(trimmed)
                    val wrapper = org.json.JSONObject()
                    wrapper.put("data", arr)
                    return wrapper
                }
            } catch (_: Exception) {}
        }
        return org.json.JSONObject()
    }

    /**
     * 检查响应是否为数组格式
     */
internal fun String.isJsonArray(): Boolean {
        return this.trim().startsWith("[")
    }

    /**
     * Map 转 JSON 字符串
     */
internal fun Map<String, Any>.toJson(): String = toJsonObject()

    /**
     * Serializes nested request maps without unchecked casts. The API request
     * payloads are assembled from maps at several call sites, so preserving
     * arbitrary key types here is safer than assuming every nested map is
     * exactly Map<String, Any>.
     */
internal fun Map<*, *>.toJsonObject(): String = buildString {
        append('{')
        entries.forEachIndexed { index, (key, value) ->
            if (index > 0) append(',')
            append('\"')
            append(key?.toString().orEmpty().escapeJson())
            append("\":")
            append(value.toJsonValue())
        }
        append('}')
    }

internal fun Any?.toJsonValue(): String = when (this) {
        null -> "null"
        is String -> "\"${escapeJson()}\""
        is Number, is Boolean -> toString()
        is Map<*, *> -> toJsonObject()
        is Iterable<*> -> joinToString(prefix = "[", postfix = "]") { it.toJsonValue() }
        else -> "\"${toString().escapeJson()}\""
    }

    /**
     * JSON 字符串转 Map
     */
internal fun String.toMapJson(): String {
        return "{$this}"
    }
