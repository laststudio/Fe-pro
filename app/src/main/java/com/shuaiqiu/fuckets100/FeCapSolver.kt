package com.shuaiqiu.fuckets100

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.Inflater

internal data class CapInstrumentationResult(
    val nonce: String,
    val state: Map<String, Int>,
    val timestamp: Long = System.currentTimeMillis(),
)

internal class FeCapSolver(
    private val endpoint: String = DEFAULT_ENDPOINT,
) {
    companion object {
        const val DEFAULT_ENDPOINT = "https://lastudio.cc/cap/f0d2ec6d51/"
        private const val TIMEOUT_MS = 10_000
    }

    suspend fun solve(onProgress: (Float) -> Unit = {}): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(0.05f)
            val challengeUrl = endpoint.trimEnd('/') + "/challenge"
            val challengeResponse = httpPost(challengeUrl, null)
            val challengeJson = JSONObject(challengeResponse)

            if (challengeJson.has("error") && !challengeJson.isNull("error")) {
                throw IllegalStateException("获取验证挑战失败: " + challengeJson.getString("error"))
            }

            val token = challengeJson.getString("token")
            val challengeObj = challengeJson.getJSONObject("challenge")
            val count = challengeObj.getInt("c")
            val saltLen = challengeObj.getInt("s")
            val targetLen = challengeObj.getInt("d")
            val instrumentationStr = if (challengeJson.has("instrumentation") && !challengeJson.isNull("instrumentation")) {
                challengeJson.getString("instrumentation")
            } else {
                null
            }

            onProgress(0.15f)

            // Solve Proof of Work in parallel on Dispatchers.Default
            val completedCount = AtomicInteger(0)
            val solutions = withContext(Dispatchers.Default) {
                (1..count).map { t ->
                    async {
                        val salt = generatePrngHex("${token}$t", saltLen)
                        val target = generatePrngHex("${token}${t}d", targetLen)
                        val nonce = solveOne(salt, target)
                        val done = completedCount.incrementAndGet()
                        val ratio = done.toFloat() / count.toFloat()
                        onProgress(0.15f + ratio * 0.70f)
                        nonce
                    }
                }.awaitAll()
            }

            // Solve instrumentation if present
            var instrResult: CapInstrumentationResult? = null
            if (!instrumentationStr.isNullOrBlank()) {
                val compressedBytes = Base64.getDecoder().decode(instrumentationStr)
                val scriptText = decompressRawDeflate(compressedBytes)
                instrResult = solveInstrumentation(scriptText)
            }
            onProgress(0.92f)

            // Redeem verification token
            val redeemUrl = endpoint.trimEnd('/') + "/redeem"
            val redeemBody = JSONObject().apply {
                put("token", token)
                put("solutions", JSONArray(solutions))
                if (instrResult != null) {
                    val instrObj = JSONObject().apply {
                        put("i", instrResult.nonce)
                        put("state", JSONObject(instrResult.state))
                        put("ts", instrResult.timestamp)
                    }
                    put("instr", instrObj)
                }
            }

            val redeemResponse = httpPost(redeemUrl, redeemBody.toString())
            val redeemJson = JSONObject(redeemResponse)

            if (!redeemJson.optBoolean("success", false)) {
                val err = redeemJson.optString("error", "验证未通过，请重试")
                throw IllegalStateException(err)
            }

            val solvedToken = redeemJson.getString("token")
            onProgress(1.0f)
            solvedToken
        }
    }

    private fun httpPost(urlStr: String, body: String?): String {
        val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Origin", "https://lastudio.cc")
            setRequestProperty("Referer", "https://lastudio.cc/")
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        return try {
            if (body != null) {
                connection.outputStream.bufferedWriter().use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val resp = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val err = runCatching { JSONObject(resp).optString("error") }.getOrNull()
                throw IllegalStateException(err?.takeIf { it.isNotBlank() } ?: "HTTP $status: $resp")
            }
            resp
        } finally {
            connection.disconnect()
        }
    }
}

internal fun fnv1a(str: String): Int {
    var h = 0x811c9dc5.toInt()
    for (i in 0 until str.length) {
        h = h xor str[i].code
        h += (h shl 1) + (h shl 4) + (h shl 7) + (h shl 8) + (h shl 24)
    }
    return h
}

internal fun generatePrngHex(seedStr: String, length: Int): String {
    var state = fnv1a(seedStr)
    val sb = StringBuilder()
    while (sb.length < length) {
        state = state xor (state shl 13)
        state = state xor (state ushr 17)
        state = state xor (state shl 5)
        val u = state.toLong() and 0xFFFFFFFFL
        val hex = u.toString(16).padStart(8, '0')
        sb.append(hex)
    }
    return sb.substring(0, length)
}

internal fun solveOne(salt: String, target: String): Long {
    val o = 4 * target.length
    val a = o / 8
    val l = o % 8
    val paddedTarget = if (target.length % 2 == 0) target else target + "0"
    val c = paddedTarget.length / 2
    val f = ByteArray(c)
    for (e in 0 until c) {
        f[e] = paddedTarget.substring(2 * e, 2 * e + 2).toInt(16).toByte()
    }
    val g = if (l > 0) ((0xFF shl (8 - l)) and 0xFF).toByte() else 0.toByte()

    val md = MessageDigest.getInstance("SHA-256")
    val saltBytes = salt.toByteArray(Charsets.UTF_8)
    val nonceBuf = ByteArray(24)
    var nonce = 0L

    while (true) {
        md.reset()
        md.update(saltBytes)
        val len = appendLongAscii(nonce, nonceBuf)
        md.update(nonceBuf, 0, len)
        val hash = md.digest()

        var match = true
        for (e in 0 until a) {
            if (hash[e] != f[e]) {
                match = false
                break
            }
        }
        if (match && l > 0) {
            val hByte = hash[a].toInt() and 0xFF
            val fByte = f[a].toInt() and 0xFF
            val gByte = g.toInt() and 0xFF
            if ((hByte and gByte) != (fByte and gByte)) {
                match = false
            }
        }
        if (match) {
            return nonce
        }
        nonce++
    }
}

private fun appendLongAscii(value: Long, buf: ByteArray): Int {
    if (value == 0L) {
        buf[0] = '0'.code.toByte()
        return 1
    }
    var v = value
    var len = 0
    while (v > 0) {
        len++
        v /= 10
    }
    v = value
    for (i in len - 1 downTo 0) {
        buf[i] = ('0'.code + (v % 10).toInt()).toByte()
        v /= 10
    }
    return len
}

internal fun domSumMock(x: Int, y: Int, z: Int): Int {
    class Node(val parentNode: Node?, val innerText: Int)

    fun buildChain(parent: Node?, valArg: Int): Node? {
        var cur = parent
        var v = valArg
        for (i in 0 until 8) {
            val child = Node(cur, v)
            if ((v and 1) == 0) {
                cur = child
            }
            v = v shr 1
        }
        return cur
    }

    val root = Node(null, 0)
    val leaf = buildChain(buildChain(buildChain(root, x), y), z)

    var sum = 0
    var node = leaf
    while (node != null && node !== root) {
        sum += node.innerText
        node = node.parentNode
    }
    return sum % 256
}

internal fun fnHelper(a: Int, b: Int, c: Int): Int {
    return (b xor a) or (c xor b)
}

internal fun decompressRawDeflate(compressed: ByteArray): String {
    val inflater = Inflater(true)
    inflater.setInput(compressed)
    val outputStream = ByteArrayOutputStream()
    val buffer = ByteArray(4096)
    while (!inflater.finished()) {
        val count = inflater.inflate(buffer)
        if (count > 0) {
            outputStream.write(buffer, 0, count)
        } else if (inflater.needsInput()) {
            break
        }
    }
    inflater.end()
    return outputStream.toString("UTF-8")
}

internal fun solveInstrumentation(scriptText: String): CapInstrumentationResult {
    val nonceRegex = Regex("""nonce:\s*["']([a-f0-9]+)["']""")
    val nonce = nonceRegex.find(scriptText)?.groupValues?.get(1)
        ?: error("Cannot find nonce in instrumentation")

    val varDeclRegex = Regex("""var\s+([a-zA-Z0-9_$]+)=(\d+);var\s+([a-zA-Z0-9_$]+)=(\d+);var\s+([a-zA-Z0-9_$]+)=(\d+);var\s+([a-zA-Z0-9_$]+)=(\d+);""")
    val varDeclMatch = varDeclRegex.find(scriptText)
        ?: error("Cannot find variable declarations in instrumentation")

    val vars = listOf(
        varDeclMatch.groupValues[1],
        varDeclMatch.groupValues[3],
        varDeclMatch.groupValues[5],
        varDeclMatch.groupValues[7],
    )
    val vals = mutableMapOf(
        vars[0] to varDeclMatch.groupValues[2].toInt(),
        vars[1] to varDeclMatch.groupValues[4].toInt(),
        vars[2] to varDeclMatch.groupValues[6].toInt(),
        vars[3] to varDeclMatch.groupValues[8].toInt(),
    )

    val h1Regex = Regex("""function\s+([a-zA-Z0-9_$]+)\s*\([^)]*\)\s*\{[^}]*this\.v\s*=""")
    val h1Name = h1Regex.find(scriptText)?.groupValues?.get(1)
        ?: error("Cannot find fnHelper in instrumentation")

    val domRegex = Regex("""function\s+([a-zA-Z0-9_$]+)\s*\([^)]*\)\s*\{[^}]*document\.createElement""")
    val domName = domRegex.find(scriptText)?.groupValues?.get(1)
        ?: error("Cannot find domHelper in instrumentation")

    val uaRegex = Regex("""${vars[0]}\s*=\s*${vars[0]}\s*\^\s*\(navigator\.userAgent\s*\?\s*(\d+)\s*:\s*(\d+)\);""")
    val uaMatch = uaRegex.find(scriptText)
        ?: error("Cannot find userAgent check in instrumentation")
    val uaKey = uaMatch.groupValues[1].toInt()
    vals[vars[0]] = (vals[vars[0]]!! xor uaKey)

    val uaEnd = uaMatch.range.last + 1
    val saltRegex = Regex("""${vars[0]}=\(\(${vars[0]}\^(\d+)\)&0x7FFFFFFF\)%900000\+100000;""")
    val saltMatch = saltRegex.find(scriptText)
        ?: error("Cannot find salt start in instrumentation")

    val opsSegment = scriptText.substring(uaEnd, saltMatch.range.first)
    val ops = opsSegment.split(";").map { it.trim() }.filter { it.isNotEmpty() }

    val opNotAndRegex = Regex("""^([a-zA-Z0-9_$]+)\s*=\s*~\(\1\s*&\s*([a-zA-Z0-9_$]+)\)$""")
    val opXorRegex = Regex("""^([a-zA-Z0-9_$]+)\s*=\s*\1\s*\^\s*([a-zA-Z0-9_$]+)$""")
    val opOrRegex = Regex("""^([a-zA-Z0-9_$]+)\s*=\s*\1\s*\|\s*([a-zA-Z0-9_$]+)$""")
    val opAndRegex = Regex("""^([a-zA-Z0-9_$]+)\s*=\s*\1\s*&\s*([a-zA-Z0-9_$]+)$""")
    val opH1Regex = Regex("""^([a-zA-Z0-9_$]+)\s*=\s*$h1Name\(([a-zA-Z0-9_$]+),\s*([a-zA-Z0-9_$]+),\s*\1\)$""")
    val opDomRegex = Regex("""^([a-zA-Z0-9_$]+)\s*=\s*$domName\(([a-zA-Z0-9_$]+),\s*([a-zA-Z0-9_$]+),\s*([a-zA-Z0-9_$]+)\)$""")

    for (op in ops) {
        var matched = false
        opNotAndRegex.find(op)?.let {
            val d = it.groupValues[1]
            val s1 = it.groupValues[2]
            vals[d] = (vals[d]!! and vals[s1]!!).inv()
            matched = true
        }
        if (matched) continue

        opXorRegex.find(op)?.let {
            val d = it.groupValues[1]
            val s1 = it.groupValues[2]
            vals[d] = vals[d]!! xor vals[s1]!!
            matched = true
        }
        if (matched) continue

        opOrRegex.find(op)?.let {
            val d = it.groupValues[1]
            val s1 = it.groupValues[2]
            vals[d] = vals[d]!! or vals[s1]!!
            matched = true
        }
        if (matched) continue

        opAndRegex.find(op)?.let {
            val d = it.groupValues[1]
            val s1 = it.groupValues[2]
            vals[d] = vals[d]!! and vals[s1]!!
            matched = true
        }
        if (matched) continue

        opH1Regex.find(op)?.let {
            val d = it.groupValues[1]
            val s1 = it.groupValues[2]
            val s2 = it.groupValues[3]
            vals[d] = fnHelper(vals[s1]!!, vals[s2]!!, vals[d]!!)
            matched = true
        }
        if (matched) continue

        opDomRegex.find(op)?.let {
            val d = it.groupValues[1]
            val s1 = it.groupValues[2]
            val s2 = it.groupValues[3]
            val s3 = it.groupValues[4]
            vals[d] = domSumMock(vals[s1]!!, vals[s2]!!, vals[s3]!!)
            matched = true
        }
    }

    for (i in 0 until 4) {
        val sRegex = Regex("""${vars[i]}=\(\(${vars[i]}\^(\d+)\)&0x7FFFFFFF\)%900000\+100000;""")
        val sVal = sRegex.find(scriptText)?.groupValues?.get(1)?.toInt()
            ?: error("Cannot find salt for ${vars[i]}")
        vals[vars[i]] = (((vals[vars[i]]!! xor sVal) and 0x7FFFFFFF) % 900000) + 100000
    }

    return CapInstrumentationResult(nonce, vals)
}
