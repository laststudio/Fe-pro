package com.shuaiqiu.fuckets100.feature.read

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.ETS100AnswerReader
import com.shuaiqiu.fuckets100.PaperSourceExporter
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.layout.heightIn
import com.shuaiqiu.fuckets100.PaperSource
import kotlinx.coroutines.launch

private const val TAG = "ReadScreen"

@Composable
internal fun CloudDirectoryExportDialogHost(
    currentPapers: List<ETS100AnswerReader.Paper>,
    isDownloadInProgress: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirmation by remember { mutableStateOf(true) }
    var inspection by remember { mutableStateOf<PaperSourceExporter.CloudDirectoryInspection?>(null) }
    var inspectionError by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    fun shareExport(result: PaperSourceExporter.LocalDirectoryExportResult) {
        Toast.makeText(context, "已保存到 下载/Fe/${result.fileName}", Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享云端缓存文件")) }
            .onFailure { Toast.makeText(context, "文件已保存，但没有可用的分享应用", Toast.LENGTH_LONG).show() }
    }

    fun inspectAndExport() {
        if (isPreparing || isExporting) return
        showConfirmation = false
        inspection = null
        inspectionError = null
        isPreparing = true
        val papersSnapshot = currentPapers.toList()
        scope.launch {
            runCatching {
                PaperSourceExporter.inspectCloudDirectoryExport(
                    context = context,
                    currentPapers = papersSnapshot,
                    isDownloadInProgress = isDownloadInProgress
                )
            }.onSuccess { result ->
                inspection = result
                if (result.canExport) {
                    isPreparing = false
                    isExporting = true
                    runCatching { PaperSourceExporter.exportCloudDirectory(context, result) }
                        .onSuccess(::shareExport)
                        .onFailure { error ->
                            if (error is CancellationException) throw error
                            Log.e(TAG, "云端缓存导出失败", error)
                            Toast.makeText(context, "导出失败：${error.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
                        }
                    isExporting = false
                    onDismissRequest()
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "云端缓存预检失败", error)
                inspectionError = error.message ?: "未知错误"
            }
            isPreparing = false
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) inspectAndExport()
        else Toast.makeText(context, "需要存储权限才能写入下载文件夹", Toast.LENGTH_LONG).show()
    }

    fun beginExport() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            inspectAndExport()
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.Archive, contentDescription = null) },
            title = { Text("导出云端缓存文件？") },
            text = {
                Text(
                    "将导出云端缓存目录中的原始 ZIP、解压目录和当前缓存文件，并保留目录结构。\n\n" +
                        "请仅在需要向作者反馈无法解析的云端题目时使用；不需要的缓存请先清除后重新下载目标作业。"
                )
            },
            confirmButton = { TextButton(onClick = ::beginExport) { Text("确认导出") } },
            dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } }
        )
    }

    inspection?.takeUnless { it.canExport }?.let { result ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(if (result.isEmpty) "未找到云端缓存文件" else "当前缓存不适合导出") },
            text = {
                Text(
                    when {
                        result.isEmpty -> "请先完整下载需要反馈的云端作业。"
                        result.isDownloadInProgress -> "云端作业仍在下载，请等待下载结束后再导出。"
                        result.hasTooManyFiles -> "云端缓存文件数量过多，请清除缓存后只下载需要反馈的作业。"
                        else -> buildString {
                            append("检测到")
                            if (result.hasTooManyParsedPapers) append("当前已解析的 ${result.parsedPaperCount} 套试卷")
                            if (result.hasTooManyParsedPapers && result.exceedsSizeLimit) append("，且")
                            if (result.exceedsSizeLimit) append(" 云端缓存总大小超过 150 MiB")
                            append("。\n\n请清除缓存后，只下载需要反馈的无法解析作业，再重新导出。")
                        }
                    }
                )
            },
            confirmButton = { TextButton(onClick = onDismissRequest) { Text("我知道了") } }
        )
    }

    inspectionError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null) },
            title = { Text("无法检查云端缓存") },
            text = { Text("预检失败：$error") },
            confirmButton = { TextButton(onClick = onDismissRequest) { Text("关闭") } }
        )
    }

    if (isPreparing || isExporting) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Column {
                        Text(if (isExporting) "正在打包云端缓存" else "正在检查云端缓存", style = MaterialTheme.typography.titleMedium)
                        Text("请勿关闭页面", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
