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

internal const val CLOUD_DIRECTORY_EXPORT_MAX_FILES = 10_000


/**
 * PaperSourceExporter 的 ZIP/目录写入实现。
 */
internal fun ets100WriteLocalSources(context: Context, source: PaperSource.Local, zip: ZipOutputStream) {
        val reader = ETS100FileReader.getReader(source.mode, context)
        val includedResources = source.resourceDirectoryNames.filter { ets100IsSafeName(it) }.toSet()
        includedResources.forEach { directoryName ->
            val path = "${ETS100FileReader.Path.getResourceDir()}/$directoryName"
            if (reader.exists(path) && !reader.isSymbolicLink(path)) {
                ets100WriteDirectory(reader, path, "$directoryName/", zip)
            }
        }

        ets100FindRelatedDataIndexes(reader, includedResources).forEach { index ->
            val path = "${ETS100FileReader.Path.getDataDir()}/${index.fileName}"
            ets100WriteStreamEntry(zip, ets100UniqueDataEntryName(index.fileName, includedResources)) {
                reader.openInputStream(path)
            }
        }
    }

internal fun ets100CollectLocalDataFiles(
        reader: ETS100FileReader.Reader,
        sourcePath: String,
        files: MutableList<PaperSourceExporter.LocalDirectoryFile>,
        skipped: MutableList<String>
    ) {
        if (!reader.exists(sourcePath)) return
        if (reader.isSymbolicLink(sourcePath)) {
            skipped += "data/（符号链接目录，已跳过）"
            return
        }
        reader.listFiles(sourcePath).forEach { child ->
            if (!ets100IsSafeName(child.name)) {
                skipped += "data/${child.name}（非法文件名，已跳过）"
                return@forEach
            }
            val childSourcePath = "$sourcePath/${child.name}"
            if (reader.isSymbolicLink(childSourcePath)) {
                skipped += "data/${child.name}（符号链接，已跳过）"
            } else if (child.isDirectory) {
                skipped += "data/${child.name}/（非文件条目，已跳过）"
            } else {
                files += PaperSourceExporter.LocalDirectoryFile(
                    sourcePath = childSourcePath,
                    archivePath = "data/${child.name}",
                    size = child.size.takeIf { it > 0L } ?: reader.getFileSize(childSourcePath)
                )
            }
        }
    }

internal fun ets100CollectLocalResourceDirectories(
        reader: ETS100FileReader.Reader,
        sourcePath: String,
        directories: MutableList<PaperSourceExporter.LocalResourceDirectory>,
        skipped: MutableList<String>
    ) {
        if (!reader.exists(sourcePath)) return
        if (reader.isSymbolicLink(sourcePath)) {
            skipped += "resource/（符号链接目录，已跳过）"
            return
        }
        reader.listFiles(sourcePath).forEach { child ->
            if (!ets100IsSafeName(child.name)) {
                skipped += "resource/${child.name}（非法文件名，已跳过）"
                return@forEach
            }
            val childSourcePath = "$sourcePath/${child.name}"
            if (reader.isSymbolicLink(childSourcePath)) {
                skipped += "resource/${child.name}（符号链接，已跳过）"
            } else if (child.isDirectory) {
                directories += PaperSourceExporter.LocalResourceDirectory(childSourcePath, child.name)
            } else {
                skipped += "resource/${child.name}（非题目目录，已跳过）"
            }
        }
    }

internal fun writeStagedLocalSources(
        stageDir: File,
        inspection: PaperSourceExporter.LocalDirectoryInspection,
        zip: ZipOutputStream
    ) {
        inspection.dataFiles.forEach { file ->
            val stagedFile = File(stageDir, file.archivePath)
            ets100WriteStreamEntry(zip, file.archivePath) { FileInputStream(stagedFile) }
        }
        inspection.resourceDirectories.forEach { directory ->
            val stagedDirectory = File(stageDir, "resource/${directory.name}")
            ets100WriteLocalDirectory(stagedDirectory, "resource/${directory.name}/", zip)
        }
    }

internal fun ets100WriteLocalDirectory(directory: File, entryPath: String, zip: ZipOutputStream) {
        if (java.nio.file.Files.isSymbolicLink(directory.toPath())) return
        val children = directory.listFiles()?.filter { ets100IsSafeName(it.name) }?.sortedBy { it.name }.orEmpty()
        if (children.isEmpty()) {
            zip.putNextEntry(ZipEntry(entryPath))
            zip.closeEntry()
            return
        }
        children.forEach { child ->
            if (java.nio.file.Files.isSymbolicLink(child.toPath())) return@forEach
            val childEntry = "$entryPath${child.name}"
            if (child.isDirectory) {
                ets100WriteLocalDirectory(child, "$childEntry/", zip)
            } else if (child.isFile) {
                ets100WriteStreamEntry(zip, childEntry) { FileInputStream(child) }
            }
        }
    }

internal fun ets100WriteCloudSources(context: Context, source: PaperSource.Cloud, zip: ZipOutputStream) {
        val cacheDir = PaperSourceExporter.cloudCacheDirectory(context, source)
        val collisions = ets100CloudFileNameCollisions(source)
        source.contents.distinctBy { it.url }.forEach { content ->
            val name = PaperSourceExporter.cloudFileName(content.url)
            val file = PaperSourceExporter.cloudCachedFile(cacheDir, content.url, name)
            if (name.isNotBlank() && name !in collisions && PaperSourceExporter.isUsableZip(file)) {
                ets100WriteStreamEntry(zip, name) { FileInputStream(file) }
            }
        }
    }

internal fun ets100CollectCloudDirectoryFiles(
        root: File,
        current: File,
        files: MutableList<PaperSourceExporter.CloudDirectoryFile>,
        skipped: MutableList<String>
    ) {
        current.listFiles()?.forEach { child ->
            if (files.size >= CLOUD_DIRECTORY_EXPORT_MAX_FILES) {
                skipped += "文件数量达到上限，停止继续扫描"
                return
            }
            val relative = child.relativeTo(root).invariantSeparatorsPath
            val canonicalChild = runCatching { child.canonicalFile }.getOrElse {
                skipped += "$relative（无法解析路径）"
                return@forEach
            }
            when {
                java.nio.file.Files.isSymbolicLink(child.toPath()) -> skipped += "$relative（符号链接）"
                !canonicalChild.toPath().startsWith(root.toPath()) -> skipped += "$relative（超出缓存目录）"
                canonicalChild.isDirectory -> ets100CollectCloudDirectoryFiles(root, canonicalChild, files, skipped)
                canonicalChild.isFile && canonicalChild.canRead() -> files += PaperSourceExporter.CloudDirectoryFile(
                    source = canonicalChild,
                    archivePath = "cloud_homework/$relative",
                    size = canonicalChild.length()
                )
                else -> skipped += "$relative（无法读取）"
            }
        } ?: skipped.add("${current.relativeTo(root).invariantSeparatorsPath.ifBlank { "cloud_homework" }}/（无法读取目录）")
    }

internal fun ets100WriteDirectory(
        reader: ETS100FileReader.Reader,
        sourcePath: String,
        entryPath: String,
        zip: ZipOutputStream
    ) {
        val children = reader.listFiles(sourcePath)
        if (children.isEmpty()) {
            zip.putNextEntry(ZipEntry(entryPath))
            zip.closeEntry()
            return
        }
        children.filter { ets100IsSafeName(it.name) }.forEach { child ->
            val childSource = "$sourcePath/${child.name}"
            if (reader.isSymbolicLink(childSource)) return@forEach
            val childEntry = "$entryPath${child.name}"
            if (child.isDirectory) {
                ets100WriteDirectory(reader, childSource, "$childEntry/", zip)
            } else {
                ets100WriteStreamEntry(zip, childEntry) { reader.openInputStream(childSource) }
            }
        }
    }

internal fun ets100WriteStreamEntry(zip: ZipOutputStream, name: String, stream: () -> java.io.InputStream?) {
        if (!ets100IsSafeZipEntry(name)) return
        val input = stream() ?: error("无法读取源文件：$name")
        input.use {
            zip.putNextEntry(ZipEntry(name))
            it.copyTo(zip)
            zip.closeEntry()
        }
    }

internal fun ets100WriteTextEntry(zip: ZipOutputStream, name: String, text: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(text.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

internal fun ets100FindRelatedDataIndexes(
        reader: ETS100FileReader.Reader,
        resourceNames: Set<String>
    ): List<PaperSourceExporter.DataIndexInfo> {
        return reader.listFiles(ETS100FileReader.Path.getDataDir())
            .filter { !it.isDirectory && ets100IsSafeName(it.name) }
            .filterNot { reader.isSymbolicLink("${ETS100FileReader.Path.getDataDir()}/${it.name}") }
            .mapNotNull { item ->
                val raw = reader.readFile("${ETS100FileReader.Path.getDataDir()}/${item.name}") ?: return@mapNotNull null
                val references = PaperSourceExporter.extractResourceReferences(raw)
                val currentReferences = references.filter { it in resourceNames }
                if (currentReferences.isEmpty()) return@mapNotNull null
                PaperSourceExporter.DataIndexInfo(
                    fileName = item.name,
                    currentPaperReferences = currentReferences,
                    otherReferences = references.filterNot { it in resourceNames }
                )
            }
    }

internal fun ets100PublishToDownloads(context: Context, source: File, fileName: String): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/zip")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Fe")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("无法创建下载文件")
            try {
                resolver.openOutputStream(uri, "w")?.use { output -> FileInputStream(source).use { it.copyTo(output) } }
                    ?: error("无法写入下载文件")
                resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                return uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }

        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Fe")
        directory.mkdirs()
        val target = File(directory, fileName)
        source.copyTo(target, overwrite = false)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
    }

    internal fun ets100CloudFileName(url: String): String =
        url.substringAfterLast('/').substringBefore('?').takeIf(::ets100IsSafeName).orEmpty()

internal fun ets100CloudFileNameCollisions(source: PaperSource.Cloud): Set<String> = source.contents
        .distinctBy { it.url }
        .groupBy { PaperSourceExporter.cloudFileName(it.url) }
        .filter { (name, contents) -> name.isBlank() || contents.map { it.url }.distinct().size > 1 }
        .keys

internal fun ets100UniqueDataEntryName(name: String, resourceNames: Set<String>): String =
        if (name in resourceNames) "data_$name" else name

internal fun ets100IsSafeName(name: String): Boolean =
        name.isNotBlank() && name != "." && name != ".." && '/' !in name && '\\' !in name

internal fun ets100IsSafeZipEntry(name: String): Boolean =
        name.isNotBlank() && !name.startsWith('/') && name.split('/').none { it == ".." }

internal fun ets100Sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }
