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
internal fun SourceExportDialogHost(
    paper: ETS100AnswerReader.Paper?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirmation by remember(paper) { mutableStateOf(paper != null) }
    var sourceInspection by remember(paper) { mutableStateOf<PaperSourceExporter.Inspection?>(null) }
    var isExportingSource by remember(paper) { mutableStateOf(false) }

    fun shareExport(result: PaperSourceExporter.ExportResult) {
        Toast.makeText(context, "已保存到 下载/Fe/${result.fileName}", Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享答案源文件")) }
            .onFailure { Toast.makeText(context, "文件已保存，但没有可用的分享应用", Toast.LENGTH_LONG).show() }
    }

    fun exportSource(allowPartial: Boolean) {
        val currentPaper = paper ?: return
        if (isExportingSource) return
        sourceInspection = null
        isExportingSource = true
        scope.launch {
            runCatching { PaperSourceExporter.export(context, currentPaper, allowPartial) }
                .onSuccess(::shareExport)
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e(TAG, "源文件导出失败", error)
                    Toast.makeText(context, "导出失败：${error.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
                }
            isExportingSource = false
            onDismissRequest()
        }
    }

    fun inspectAndExport() {
        val currentPaper = paper ?: return
        val source = currentPaper.source
        if (source == null) {
            Toast.makeText(context, "来源信息已失效，请重新读取该试卷", Toast.LENGTH_LONG).show()
            onDismissRequest()
            return
        }
        isExportingSource = true
        scope.launch {
            runCatching { PaperSourceExporter.inspect(context, source) }
                .onSuccess { inspection ->
                    if (inspection.isComplete) {
                        runCatching { PaperSourceExporter.export(context, currentPaper, false) }
                            .onSuccess(::shareExport)
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                Toast.makeText(context, "导出失败：${error.message}", Toast.LENGTH_LONG).show()
                            }
                        onDismissRequest()
                    } else {
                        sourceInspection = inspection
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Toast.makeText(context, "无法核验源文件：${error.message}", Toast.LENGTH_LONG).show()
                    onDismissRequest()
                }
            isExportingSource = false
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) inspectAndExport()
        else Toast.makeText(context, "需要存储权限才能写入下载文件夹", Toast.LENGTH_LONG).show()
    }

    fun beginSourceExport() {
        showConfirmation = false
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            inspectAndExport()
        }
    }

    if (paper != null && showConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.Archive, contentDescription = null) },
            title = { Text("导出答案源文件？") },
            text = {
                Text("源文件用于提交给作者适配新题型，不适合作为常规答案分享。\n\n应用将只打包当前整套试卷关联的源文件，并保存到 下载/Fe。")
            },
            confirmButton = { TextButton(onClick = ::beginSourceExport) { Text("确认导出") } },
            dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } }
        )
    }

    sourceInspection?.let { inspection ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("源文件不完整") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("缺少或冲突的源文件：")
                    (inspection.missing + inspection.conflicts).distinct().forEach { Text("• $it") }
                    Text("仍然导出时，导出说明中会记录以上缺失项。")
                }
            },
            confirmButton = {
                val source = paper?.source
                if (source is PaperSource.Cloud) {
                    TextButton(onClick = {
                        sourceInspection = null
                        isExportingSource = true
                        scope.launch {
                            val updated = runCatching {
                                PaperSourceExporter.downloadMissingCloudFiles(context, source)
                            }.getOrElse {
                                if (it is CancellationException) throw it
                                Toast.makeText(context, "下载缺失文件失败：${it.message}", Toast.LENGTH_LONG).show()
                                PaperSourceExporter.inspect(context, source)
                            }
                            isExportingSource = false
                            if (updated.isComplete) exportSource(false) else sourceInspection = updated
                        }
                    }) { Text("下载缺失文件") }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onDismissRequest) { Text("取消") }
                    TextButton(onClick = { exportSource(true) }) { Text("仍然导出") }
                }
            }
        )
    }

    if (isExportingSource) {
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
                        Text("正在准备源文件", style = MaterialTheme.typography.titleMedium)
                        Text("请勿关闭页面", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
