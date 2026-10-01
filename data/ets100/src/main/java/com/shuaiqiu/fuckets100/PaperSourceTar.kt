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


internal const val TAR_BLOCK_SIZE = 512

/**
 * TAR 归档解析工具，供 [PaperSourceExporter] 解包特权模式导出结果。
 */
internal fun readTarBlock(input: InputStream, block: ByteArray): Boolean {
        var offset = 0
        while (offset < block.size) {
            val count = input.read(block, offset, block.size - offset)
            if (count < 0) {
                check(offset == 0) { "tar 头部不完整" }
                return false
            }
            offset += count
        }
        return true
    }

internal fun tarText(bytes: ByteArray, offset: Int, length: Int): String {
        val end = (offset until offset + length).firstOrNull { bytes[it] == 0.toByte() } ?: offset + length
        return String(bytes, offset, end - offset, Charsets.UTF_8).trim()
    }

internal fun tarOctal(bytes: ByteArray, offset: Int, length: Int): Long {
        val text = tarText(bytes, offset, length).trim()
        return text.toLongOrNull(8) ?: 0L
    }

internal fun isSafeTarPath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith('/') && path.split('/').none { it == ".." || it.isBlank() }

internal fun copyTarBytes(input: InputStream, output: FileOutputStream, size: Long) {
        var remaining = size
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (remaining > 0L) {
            val count = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
            check(count > 0) { "tar 数据不完整" }
            output.write(buffer, 0, count)
            remaining -= count
        }
    }

internal fun skipTarBytes(input: InputStream, size: Long) {
        var remaining = size
        while (remaining > 0L) {
            val skipped = input.skip(remaining)
            if (skipped > 0L) {
                remaining -= skipped
            } else {
                check(input.read() >= 0) { "tar 数据不完整" }
                remaining--
            }
        }
    }

internal fun skipTarPadding(input: InputStream, size: Long) {
        val blockSize = TAR_BLOCK_SIZE.toLong()
        val padding = (blockSize - (size % blockSize)) % blockSize
        skipTarBytes(input, padding)
    }
