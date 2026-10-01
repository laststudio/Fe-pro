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
     * 下载文件到指定路径
     * 宝贝用于下载云端作业的 ZIP 文件喵~
     *
     * @param url 文件 URL
     * @param destFile 目标文件
     * @return 下载结果
     */
internal suspend fun ets100DownloadFile(
        url: String,
        destFile: File,
        onProgress: suspend (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "downloadFile: 开始下载 $url")
            val urlObj = URL(url)
            val connection = urlObj.openConnection() as HttpURLConnection

            // 宝贝如果是 HTTPS URL，需要处理 SSL 证书验证问题喵~
            // 因为 CDN 的 SSL 证书配置有问题（证书是发给 default.chinanetcenter.com 的）
            if (connection is javax.net.ssl.HttpsURLConnection) {
                // 创建一个信任所有证书的 TrustManager喵~
                val trustAllCerts = arrayOf(object : javax.net.ssl.X509TrustManager {
                    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                })
                val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                connection.sslSocketFactory = sslContext.socketFactory
                // 宝贝也忽略主机名验证喵~
                connection.hostnameVerifier = javax.net.ssl.HostnameVerifier { _, _ -> true }
            }

            connection.connectTimeout = ETS100ApiClient.Config.TIMEOUT_MS
            connection.readTimeout = ETS100ApiClient.Config.TIMEOUT_MS

            // 设置 CDN 请求头（参考 release.py）
            val host = if (url.contains("cdn.subject.ets100.com")) "cdn.subject.ets100.com" else "api.ets100.com"
            connection.setRequestProperty("Host", host)
            connection.setRequestProperty("User-Agent", "libcurl-agent/1.0")
            connection.setRequestProperty("Accept", "*/*")

            connection.connect()

            val responseCode = connection.responseCode
            Log.d(TAG, "downloadFile: HTTP Response = $responseCode")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 使用缓冲流提高下载效率
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L
                val totalBytes = connection.contentLengthLong

                connection.inputStream.use { input ->
                    destFile.outputStream().use { output ->
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            onProgress(totalBytesRead, totalBytes)
                        }
                    }
                }

                Log.d(TAG, "downloadFile: 下载完成 ${destFile.absolutePath}, 大小=$totalBytesRead bytes")
                Result.success(Unit)
            } else {
                // 读取错误信息
                val errorStream = connection.errorStream
                val errorMsg = if (errorStream != null) {
                    errorStream.bufferedReader().readText()
                } else {
                    "HTTP $responseCode"
                }
                Log.e(TAG, "downloadFile: 下载失败 $errorMsg")
                Result.failure(Exception("下载失败: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "downloadFile: 下载异常 ${e.message}")
            Result.failure(e)
        }
    }
