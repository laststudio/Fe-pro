package com.shuaiqiu.fuckets100

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream


internal fun formatExportSize(size: Long): String = when {
        size < 1024L -> "$size B"
        size < 1024L * 1024L -> "%.1f KiB".format(Locale.US, size / 1024.0)
        else -> "%.1f MiB".format(Locale.US, size / (1024.0 * 1024.0))
    }

internal fun buildLocalSourceReadme(
        paper: ETS100AnswerReader.Paper,
        source: PaperSource,
        inspection: PaperSourceExporter.Inspection
    ): String = buildString {
        appendLine("Fe 答案题目源文件导出")
        appendLine("本文件仅用于提交给作者适配新题型，不适合作为常规答案分享。")
        appendLine()
        appendLine("试卷：${paper.title}")
        appendLine("模式：${if (source is PaperSource.Cloud) "云端" else "本地"}")
        appendLine("导出时间：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        if (source is PaperSource.Local) {
            appendLine("说明：本地资源是官方客户端下载 ZIP 后的原始目录，不是字节级原下载 ZIP。")
            appendLine("当前试卷资源目录：${source.resourceDirectoryNames.joinToString()}")
            appendLine("附带的 data 索引：")
            if (inspection.relatedDataIndexes.isEmpty()) appendLine("- 无")
            inspection.relatedDataIndexes.forEach { index ->
                appendLine("- ${index.fileName}")
                appendLine("  当前试卷引用：${index.currentPaperReferences.joinToString()}")
                if (index.otherReferences.isNotEmpty()) {
                    appendLine("  其他引用（仅保留在原索引中，未打包对应资源）：${index.otherReferences.joinToString()}")
                }
            }
        } else if (source is PaperSource.Cloud) {
            appendLine("作业标识：${source.homeworkIdentity}")
            source.contents.forEach { appendLine("${it.groupName}: ${it.url} -> ${PaperSourceExporter.cloudFileName(it.url)}") }
        }
        appendLine()
        appendLine("预计来源：")
        inspection.expected.forEach { appendLine("- $it") }
        if (!inspection.isComplete) {
            appendLine()
            appendLine("警告：这是部分导出，以下来源缺失或冲突：")
            (inspection.missing + inspection.conflicts).distinct().forEach { appendLine("- $it") }
        }
    }
