package com.shuaiqiu.fuckets100.feature.shared

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.RootManager
import com.shuaiqiu.fuckets100.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.ZipInputStream

private const val ETS_PACKAGE = "com.ets100.secondary"
private const val FILE_PATH = "/旧版e听说/E听说中学7.0.71.zip"
private const val FALLBACK_URL = "https://zip2.webgetstore.com/694b7533e95937b1b19761d9f8ce8289/6adc1f7a/2026/10/24/018bdc12e52f5b14e6c7f1c8137bdc81.zip?fn=E%E5%90%AC%E8%AF%B4%E4%B8%AD%E5%AD%A67.0.71.zip"

private data class DowngradePackage(
    val apk: File,
    val versionName: String?,
    val versionCode: Long?,
)

private data class EtsCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)

private object EtsDowngradeInstaller {
    suspend fun downloadAndExtract(
        context: Context,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): DowngradePackage = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "ets-downgrade").apply { mkdirs() }
        val zip = File(dir, "E听说中学7.0.71.zip")
        val apk = File(dir, "E听说中学7.0.71.apk")
        if (!apk.isFile || apk.length() == 0L) {
            val url = resolveUrl()
            download(url, zip, onProgress)
            extractApk(zip, apk)
        }
        val publicApk = File("/sdcard/Download/Fe/E听说中学7.0.71.apk")
        val requestedPublicApk = File("/sdcard/download/Fe/E听说中学7.0.71.apk")
        publicApk.parentFile?.mkdirs()
        requestedPublicApk.parentFile?.mkdirs()
        apk.copyTo(publicApk, overwrite = true)
        apk.copyTo(requestedPublicApk, overwrite = true)
        val packageInfo = context.packageManager.getPackageArchiveInfo(publicApk.absolutePath, 0)
            ?: error("下载的文件不是有效 APK")
        require(packageInfo.packageName == ETS_PACKAGE) {
            "安装包包名不匹配：${packageInfo.packageName}"
        }
        DowngradePackage(
            apk = publicApk,
            versionName = packageInfo.versionName,
            versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode else packageInfo.versionCode.toLong(),
        )
    }

    suspend fun install(mode: ActivationMode, downloadedPackage: DowngradePackage): String =
        withContext(Dispatchers.IO) {
            val command = "pm install -r -d -S ${downloadedPackage.apk.length()}"
            val result = when (mode) {
                ActivationMode.ROOT -> RootManager.execAsRootWithInput(command, downloadedPackage.apk.inputStream())?.let {
                    EtsCommandResult(it.exitCode, it.stdout, it.stderr)
                }
                ActivationMode.SHIZUKU -> ShizukuManager.execCommandWithInput(command, downloadedPackage.apk.inputStream())?.let {
                    EtsCommandResult(it.exitCode, it.stdout, it.stderr)
                }
                else -> null
            } ?: error("无可用的 Root/Shizuku 安装权限")
            if (result.exitCode != 0 || !result.stdout.contains("Success", ignoreCase = true)) {
                error(buildString {
                    append("退出码=${result.exitCode}")
                    if (result.stdout.isNotBlank()) append("；输出=${result.stdout.trim()}")
                    if (result.stderr.isNotBlank()) append("；错误=${result.stderr.trim()}")
                })
            }
            result.stdout.trim()
        }

    fun openInstaller(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openUninstaller(context: Context) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:$ETS_PACKAGE")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun resolveUrl(): String = runCatching {
        val encodedPath = URLEncoder.encode(FILE_PATH, "UTF-8").replace("+", "%20")
        val connection = (URL("https://oplist.lastudio.cc/api/fs/get?path=$encodedPath").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
        }
        connection.inputStream.bufferedReader().use { reader ->
            JSONObject(reader.readText()).optJSONObject("data")?.optString("raw_url")
                ?.takeIf { it.startsWith("https://") }
                ?: error("OpenList 未返回下载地址")
        }
    }.getOrDefault(FALLBACK_URL)

    private fun download(
        url: String,
        target: File,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) {
        val temp = File(target.parentFile, "${target.name}.part").also { it.delete() }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 60_000
            instanceFollowRedirects = true
        }
        connection.inputStream.use { input ->
            val total = connection.contentLengthLong
            var downloaded = 0L
            FileOutputStream(temp).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    downloaded += count
                    onProgress(downloaded, total)
                }
            }
        }
        require(temp.length() > 1024L) { "旧版安装包下载不完整" }
        if (!temp.renameTo(target)) temp.copyTo(target, overwrite = true).also { temp.delete() }
    }

    private fun extractApk(zip: File, target: File) {
        val temp = File(target.parentFile, "${target.name}.part").also { it.delete() }
        var found = false
        ZipInputStream(zip.inputStream().buffered()).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                val safeName = entry.name.substringAfterLast('/')
                if (!entry.isDirectory && safeName.endsWith(".apk", ignoreCase = true)) {
                    FileOutputStream(temp).use { output -> input.copyTo(output) }
                    found = true
                    break
                }
            }
        }
        require(found && temp.length() > 1024L) { "旧版压缩包中没有有效 APK" }
        if (!temp.renameTo(target)) temp.copyTo(target, overwrite = true).also { temp.delete() }
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
}

/** E 听说 8.x 本地模式兼容性提示及可选降级流程。 */
@Composable
fun EtsEightSeriesLocalModeDialog(
    versionName: String,
    mode: ActivationMode,
    onDismiss: () -> Unit,
    onUseCloudMode: () -> Unit,
    onContinueLocalMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloadedPackage by remember { mutableStateOf<DowngradePackage?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadBytes by remember { mutableLongStateOf(0L) }
    var downloadTotal by remember { mutableLongStateOf(0L) }
    var busy by remember { mutableStateOf(false) }
    var installFailed by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("正在准备下载") }

    if (busy) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = {},
            icon = { Icon(Icons.Default.Download, contentDescription = null) },
            title = { Text("正在准备旧版安装包") },
            text = {
                Column {
                    Text(statusText)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (downloadTotal > 0L) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${downloadBytes / 1024 / 1024} / ${downloadTotal / 1024 / 1024} MB（${(downloadProgress * 100).toInt()}%）",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {},
        )
        return
    }

    if (installFailed && downloadedPackage == null) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("无法完成降级准备") },
            text = { Text(message ?: "旧版安装包下载或解压失败") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
        )
        return
    }

    if (installFailed && downloadedPackage != null) {
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("自动降级安装失败") },
            text = {
                Column {
                    Text("可以卸载当前 8.x 版本后再安装，或交给系统安装器处理。卸载操作可能影响 E 听说本地数据，请先确认数据已备份。")
                    message?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("失败原因：$it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    runCatching { EtsDowngradeInstaller.openInstaller(context, downloadedPackage!!.apk) }
                        .onFailure { Toast.makeText(context, "无法打开系统安装器：${it.message}", Toast.LENGTH_LONG).show() }
                }) { Text("系统安装器") }
            },
            dismissButton = {
                Column {
                    TextButton(onClick = {
                        runCatching { EtsDowngradeInstaller.openUninstaller(context) }
                            .onFailure { Toast.makeText(context, "无法打开卸载页面：${it.message}", Toast.LENGTH_LONG).show() }
                    }) { Text("先卸载旧版") }
                    TextButton(onClick = onDismiss) { Text("保留安装包") }
                }
            },
        )
        return
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("E 听说版本兼容性提示") },
        text = {
            Text("检测到 E 听说版本为 $versionName。8.x 版本暂不兼容原有 7.x 本地读取方式，使用本地模式可能无法读取到数据。建议降级到 7.x，或改用云端模式。")
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        scope.launch {
                            busy = true
                            statusText = "正在下载旧版安装包"
                            downloadProgress = 0f
                            downloadBytes = 0L
                            downloadTotal = 0L
                            runCatching {
                                EtsDowngradeInstaller.downloadAndExtract(context) { downloaded, total ->
                                    downloadBytes = downloaded
                                    downloadTotal = total
                                    if (total > 0L) downloadProgress = (downloaded.toFloat() / total).coerceIn(0f, 1f)
                                }
                            }
                                .onSuccess { downloaded ->
                                    downloadedPackage = downloaded
                                    if (mode == ActivationMode.ROOT || mode == ActivationMode.SHIZUKU) {
                                        statusText = "正在尝试保留数据降级安装"
                                        runCatching { EtsDowngradeInstaller.install(mode, downloaded) }
                                            .onSuccess {
                                                Toast.makeText(context, "已尝试降级安装 E 听说 ${downloaded.versionName}", Toast.LENGTH_LONG).show()
                                                onDismiss()
                                            }
                                            .onFailure {
                                                message = it.message
                                                installFailed = true
                                            }
                                    } else {
                                        installFailed = true
                                        message = "直读模式无法保留数据自动降级，请先卸载旧版再安装"
                                    }
                                }
                                .onFailure { message = it.message ?: "下载失败"; installFailed = true }
                            busy = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("下载并降级") }
                TextButton(
                    onClick = onUseCloudMode,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("改用云端模式") }
                TextButton(
                    onClick = onContinueLocalMode,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("继续使用本地模式") }
            }
        },
        dismissButton = {},
    )
}
