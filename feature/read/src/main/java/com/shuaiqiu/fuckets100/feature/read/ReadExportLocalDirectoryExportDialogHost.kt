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
internal fun LocalDirectoryExportDialogHost(
    mode: ActivationMode,
    currentPapers: List<ETS100AnswerReader.Paper>,
    isParsingInProgress: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirmation by remember { mutableStateOf(true) }
    var inspection by remember { mutableStateOf<PaperSourceExporter.LocalDirectoryInspection?>(null) }
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
        runCatching { context.startActivity(Intent.createChooser(intent, "分享本地题目文件")) }
            .onFailure { Toast.makeText(context, "文件已保存，但没有可用的分享应用", Toast.LENGTH_LONG).show() }
    }

    fun inspectAndExport() {
        if (isPreparing || isExporting) return
        showConfirmation = false
        inspection = null
        inspectionError = null
        isPreparing = true
        val papersSnapshot = currentPapers.toList()
        val parsingInProgressSnapshot = isParsingInProgress
        scope.launch {
            runCatching {
                PaperSourceExporter.inspectLocalDirectoryExport(
                    context = context,
                    mode = mode,
                    currentPapers = papersSnapshot,
                    isParsingInProgress = parsingInProgressSnapshot
                )
            }
                .onSuccess { result ->
                    inspection = result
                    if (result.canExport) {
                        isPreparing = false
                        isExporting = true
                        runCatching { PaperSourceExporter.exportLocalDirectory(context, result) }
                            .onSuccess(::shareExport)
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                Log.e(TAG, "本地目录导出失败", error)
                                Toast.makeText(context, "导出失败：${error.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
                            }
                        isExporting = false
                        onDismissRequest()
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e(TAG, "本地目录预检失败", error)
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
            title = { Text("导出本地题目文件？") },
            text = {
                Text(
                    "将导出当前本地目录中 data 和 resource 的全部文件，并附上文件对应关系及可解析题目结构。\n\n" +
                        "请确认目录内只保留需要提交给作者处理的内容；不需要的试卷请先删除，之后可重新下载。\n\n" +
                        "导出时间可能较长，请耐心等待。"
                )
            },
            confirmButton = { TextButton(onClick = ::beginExport) { Text("确认导出") } },
            dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } }
        )
    }

    inspection?.takeUnless { it.canExport }?.let { result ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    if (result.isEmpty) "未找到可导出的题目文件" else "当前目录不适合导出"
                )
            },
            text = {
                val text = when {
                    result.isEmpty ->
                        "请先完整下载需要反馈的题目文件。若已下载但仍未检测到文件，可能是该试卷类型暂不支持，或读取出现异常；请截图题目类型后反馈作者。"
                    else -> buildString {
                        append("检测到")
                        if (result.hasTooManyParsedPapers) {
                            append("当前已解析的 ${result.parsedPaperCount} 套试卷")
                        }
                        if (result.hasTooManyParsedPapers && result.exceedsSizeLimit) append("，且")
                        if (result.exceedsSizeLimit) {
                            append(" 本地文件总大小超过 150 MiB")
                        }
                        if (result.isParsingInProgress && result.hasTooManyParsedPapers) {
                            append("；后台解析仍在进行，以上数量不是最终结果")
                        }
                        append("。\n\n仅保留需要答案或无法解析的内容。已可解析且不需要额外处理的试卷请不要导出；文件过多请分批处理。整理方法：全部删除后，手动完整下载需要提交给作者的无法解析试卷。")
                    }
                }
                Text(text)
            },
            confirmButton = { TextButton(onClick = onDismissRequest) { Text("我知道了") } }
        )
    }

    inspectionError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null) },
            title = { Text("无法检查本地目录") },
            text = { Text("预检失败：$error") },
            confirmButton = { TextButton(onClick = onDismissRequest) { Text("关闭") } }
        )
    }

    if (isPreparing || isExporting) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Column {
                        Text(if (isExporting) "正在打包题目文件" else "正在检查本地目录", style = MaterialTheme.typography.titleMedium)
                        Text("请勿关闭页面", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
