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

object PaperSourceExporter {
    data class Inspection(
        val expected: List<String>,
        val missing: List<String>,
        val conflicts: List<String> = emptyList(),
        val relatedDataIndexes: List<DataIndexInfo> = emptyList()
    ) {
        val isComplete: Boolean get() = missing.isEmpty() && conflicts.isEmpty()
    }

    data class DataIndexInfo(
        val fileName: String,
        val currentPaperReferences: List<String>,
        val otherReferences: List<String>
    )

    data class ExportResult(
        val uri: Uri,
        val fileName: String,
        val missing: List<String>
    )

    data class LocalDirectoryFile(
        val sourcePath: String,
        val archivePath: String,
        val size: Long
    )

    data class LocalResourceDirectory(
        val sourcePath: String,
        val name: String
    )

    data class LocalDirectoryInspection(
        val mode: ActivationMode,
        val dataFiles: List<LocalDirectoryFile>,
        val resourceDirectories: List<LocalResourceDirectory>,
        val skipped: List<String>,
        val totalBytes: Long,
        val parsedPapers: List<ETS100AnswerReader.Paper>,
        val isParsingInProgress: Boolean = false
    ) {
        val parsedPaperCount: Int get() = parsedPapers.size
        val isEmpty: Boolean get() = dataFiles.isEmpty() && resourceDirectories.isEmpty()
        val exceedsSizeLimit: Boolean get() = totalBytes > LOCAL_DIRECTORY_EXPORT_MAX_BYTES
        val hasTooManyParsedPapers: Boolean get() = parsedPaperCount >= LOCAL_DIRECTORY_EXPORT_MAX_PARSED_PAPERS
        val blockReason: LocalDirectoryExportBlockReason?
            get() = localDirectoryExportBlockReason(
                dataFiles.size + resourceDirectories.size,
                totalBytes,
                parsedPaperCount
            )
        val canExport: Boolean get() = blockReason == null
    }

    enum class LocalDirectoryExportBlockReason {
        EMPTY,
        TOO_MANY_PARSED_PAPERS,
        TOO_LARGE
    }

    data class LocalDirectoryExportResult(
        val uri: Uri,
        val fileName: String
    )

    data class CloudDirectoryFile(
        val source: File,
        val archivePath: String,
        val size: Long
    )

    data class CloudDirectoryInspection(
        val root: File,
        val files: List<CloudDirectoryFile>,
        val skipped: List<String>,
        val totalBytes: Long,
        val parsedPapers: List<ETS100AnswerReader.Paper>,
        val isDownloadInProgress: Boolean
    ) {
        val parsedPaperCount: Int get() = parsedPapers.size
        val isEmpty: Boolean get() = files.isEmpty()
        val exceedsSizeLimit: Boolean get() = totalBytes > LOCAL_DIRECTORY_EXPORT_MAX_BYTES
        val hasTooManyParsedPapers: Boolean get() = parsedPaperCount >= LOCAL_DIRECTORY_EXPORT_MAX_PARSED_PAPERS
        val hasTooManyFiles: Boolean get() = files.size >= CLOUD_DIRECTORY_EXPORT_MAX_FILES
        val blockReason: LocalDirectoryExportBlockReason?
            get() = localDirectoryExportBlockReason(files.size, totalBytes, parsedPaperCount)
        val canExport: Boolean get() = blockReason == null && !hasTooManyFiles && !isDownloadInProgress
    }

    suspend fun inspect(context: Context, source: PaperSource): Inspection = withContext(Dispatchers.IO) {
        when (source) {
            is PaperSource.Local -> inspectLocal(context, source)
            is PaperSource.Cloud -> inspectCloud(context, source)
        }
    }

    suspend fun downloadMissingCloudFiles(
        context: Context,
        source: PaperSource.Cloud
    ): Inspection = withContext(Dispatchers.IO) {
        val cacheDir = cloudCacheDirectory(context, source).apply { mkdirs() }
        val collisions = ets100CloudFileNameCollisions(source)
        source.contents.distinctBy { it.url }.forEach { content ->
            val fileName = cloudFileName(content.url)
            if (fileName.isBlank() || fileName in collisions) return@forEach
            val target = cloudCachedFile(cacheDir, content.url, fileName)
            if (isUsableZip(target)) return@forEach
            target.delete()
            val temporary = File(cacheDir, "${target.name}.part").also { it.delete() }
            val result = ETS100ApiClient.downloadFile(resolveCloudUrl(source.baseUrl, content.url), temporary)
            if (result.isSuccess && isUsableZip(temporary)) {
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
            } else {
                temporary.delete()
            }
        }
        inspectCloud(context, source)
    }

    suspend fun export(
        context: Context,
        paper: ETS100AnswerReader.Paper,
        allowPartial: Boolean
    ): ExportResult = withContext(Dispatchers.IO) {
        val source = paper.source ?: error("试卷来源信息已失效，请重新读取该试卷")
        val inspection = when (source) {
            is PaperSource.Local -> inspectLocal(context, source)
            is PaperSource.Cloud -> inspectCloud(context, source)
        }
        if (!allowPartial && !inspection.isComplete) {
            error("源文件不完整")
        }

        val fileName = buildExportFileName(paper.title)
        val stagingDir = File(context.cacheDir, "source_exports").apply { mkdirs() }
        val stagingFile = File(stagingDir, "$fileName.part").also { it.delete() }
        try {
            ZipOutputStream(FileOutputStream(stagingFile).buffered()).use { zip ->
                when (source) {
                    is PaperSource.Local -> ets100WriteLocalSources(context, source, zip)
                    is PaperSource.Cloud -> ets100WriteCloudSources(context, source, zip)
                }
                ets100WriteTextEntry(zip, "导出说明.txt", buildLocalSourceReadme(paper, source, inspection))
            }
            ets100PublishToDownloads(context, stagingFile, fileName).let { uri ->
                ExportResult(uri, fileName, inspection.missing + inspection.conflicts)
            }
        } finally {
            stagingFile.delete()
        }
    }

    suspend fun inspectLocalDirectoryExport(
        context: Context,
        mode: ActivationMode,
        currentPapers: List<ETS100AnswerReader.Paper>,
        isParsingInProgress: Boolean
    ): LocalDirectoryInspection = withContext(Dispatchers.IO) {
        require(mode != ActivationMode.CLOUD) { "云端模式不支持本地目录导出" }
        val reader = ETS100FileReader.getReader(mode, context)
        val dataPath = ETS100FileReader.Path.getDataDir()
        val resourcePath = ETS100FileReader.Path.getResourceDir()
        val totalBytes = reader.getDirectorySize(dataPath) + reader.getDirectorySize(resourcePath)
        val parsedPapers = completedParsedPapers(currentPapers)
        if (totalBytes > LOCAL_DIRECTORY_EXPORT_MAX_BYTES) {
            return@withContext LocalDirectoryInspection(
                mode = mode,
                dataFiles = emptyList(),
                resourceDirectories = emptyList(),
                skipped = emptyList(),
                totalBytes = totalBytes,
                parsedPapers = parsedPapers,
                isParsingInProgress = isParsingInProgress
            )
        }

        val dataFiles = mutableListOf<LocalDirectoryFile>()
        val resourceDirectories = mutableListOf<LocalResourceDirectory>()
        val skipped = mutableListOf<String>()
        ets100CollectLocalDataFiles(
            reader = reader,
            sourcePath = dataPath,
            files = dataFiles,
            skipped = skipped
        )
        ets100CollectLocalResourceDirectories(
            reader = reader,
            sourcePath = resourcePath,
            directories = resourceDirectories,
            skipped = skipped
        )
        LocalDirectoryInspection(
            mode = mode,
            dataFiles = dataFiles.sortedBy { it.archivePath },
            resourceDirectories = resourceDirectories.sortedBy { it.name },
            skipped = skipped.sorted(),
            totalBytes = totalBytes,
            parsedPapers = parsedPapers,
            isParsingInProgress = isParsingInProgress
        )
    }

    suspend fun exportLocalDirectory(
        context: Context,
        inspection: LocalDirectoryInspection
    ): LocalDirectoryExportResult = withContext(Dispatchers.IO) {
        check(inspection.canExport) { "当前本地目录不符合导出条件" }
        val reader = ETS100FileReader.getReader(inspection.mode, context)
        val fileName = buildLocalDirectoryExportFileName()
        val stagingDir = File(context.cacheDir, "source_exports").apply { mkdirs() }
        val stagingFile = File(stagingDir, "$fileName.part").also { it.delete() }
        val localStageDir = File(stagingDir, "local_stage_${System.nanoTime()}")
        try {
            val staged = stagePrivilegedLocalSources(inspection, localStageDir)
            ZipOutputStream(FileOutputStream(stagingFile).buffered()).use { zip ->
                if (staged) {
                    writeStagedLocalSources(localStageDir, inspection, zip)
                } else {
                    inspection.dataFiles.forEach { file ->
                        ets100WriteStreamEntry(zip, file.archivePath) { reader.openInputStream(file.sourcePath) }
                    }
                    inspection.resourceDirectories.forEach { directory ->
                        ets100WriteDirectory(reader, directory.sourcePath, "resource/${directory.name}/", zip)
                    }
                }
                ets100WriteTextEntry(zip, "导出说明.txt", buildLocalDirectoryReadme(inspection))
            }
            LocalDirectoryExportResult(ets100PublishToDownloads(context, stagingFile, fileName), fileName)
        } finally {
            stagingFile.delete()
            localStageDir.deleteRecursively()
        }
    }

    suspend fun inspectCloudDirectoryExport(
        context: Context,
        currentPapers: List<ETS100AnswerReader.Paper>,
        isDownloadInProgress: Boolean
    ): CloudDirectoryInspection = withContext(Dispatchers.IO) {
        val expectedRoot = File(context.cacheDir.canonicalFile, "cloud_homework")
        val root = expectedRoot.canonicalFile
        val files = mutableListOf<CloudDirectoryFile>()
        val skipped = mutableListOf<String>()
        val rootIsSafe = !java.nio.file.Files.isSymbolicLink(expectedRoot.toPath()) &&
            root.toPath().startsWith(context.cacheDir.canonicalFile.toPath())
        if (!rootIsSafe) {
            skipped += "cloud_homework（缓存根目录无效）"
        } else if (root.isDirectory) {
            ets100CollectCloudDirectoryFiles(root, root, files, skipped)
        }
        CloudDirectoryInspection(
            root = root,
            files = files.sortedBy { it.archivePath },
            skipped = skipped.sorted(),
            totalBytes = files.sumOf { it.size },
            parsedPapers = completedParsedPapers(currentPapers),
            isDownloadInProgress = isDownloadInProgress
        )
    }

    suspend fun exportCloudDirectory(
        context: Context,
        inspection: CloudDirectoryInspection
    ): LocalDirectoryExportResult = withContext(Dispatchers.IO) {
        check(inspection.canExport) { "当前云端缓存目录不符合导出条件" }
        val expectedRoot = File(context.cacheDir.canonicalFile, "cloud_homework")
        check(!java.nio.file.Files.isSymbolicLink(expectedRoot.toPath())) { "云端缓存根目录已变为符号链接" }
        check(expectedRoot.canonicalFile == inspection.root) { "云端缓存根目录已发生变化" }
        val fileName = buildCloudDirectoryExportFileName()
        val stagingDir = File(context.cacheDir, "source_exports").apply { mkdirs() }
        val stagingFile = File(stagingDir, "$fileName.part").also { it.delete() }
        try {
            ZipOutputStream(FileOutputStream(stagingFile).buffered()).use { zip ->
                var writtenBytes = 0L
                inspection.files.forEach { file ->
                    val source = file.source
                    val canonicalSource = source.canonicalFile
                    check(!java.nio.file.Files.isSymbolicLink(source.toPath())) { "缓存文件已变为符号链接" }
                    check(canonicalSource.toPath().startsWith(inspection.root.toPath())) { "缓存文件超出云端缓存目录" }
                    check(canonicalSource.isFile && canonicalSource.canRead()) { "缓存文件已失效" }
                    writtenBytes += canonicalSource.length()
                    check(writtenBytes <= LOCAL_DIRECTORY_EXPORT_MAX_BYTES) { "云端缓存总大小超过 150 MiB" }
                    ets100WriteStreamEntry(zip, file.archivePath) { FileInputStream(canonicalSource) }
                }
                ets100WriteTextEntry(zip, "导出说明.txt", buildCloudDirectoryReadme(inspection))
            }
            LocalDirectoryExportResult(ets100PublishToDownloads(context, stagingFile, fileName), fileName)
        } finally {
            stagingFile.delete()
        }
    }

    fun cloudCacheDirectory(context: Context, source: PaperSource.Cloud): File {
        val urls = source.contents.map { resolveCloudUrl(source.baseUrl, it.url) }.sorted().joinToString("|")
        val identity = "${source.status}:${source.homeworkIdentity}:${source.baseUrl}:$urls"
        return File(File(context.cacheDir, "cloud_homework"), ets100Sha256(identity).take(24))
    }

    fun cloudCacheDirectory(
        context: Context,
        status: String,
        homeworkIdentity: String,
        baseUrl: String,
        contents: List<CloudContent>
    ): File = cloudCacheDirectory(
        context,
        PaperSource.Cloud(status, homeworkIdentity, baseUrl, contents)
    )

    fun cloudCachedFile(cacheDir: File, url: String, displayName: String = cloudFileName(url)): File =
        File(cacheDir, "${ets100Sha256(url).take(16)}_$displayName")

    internal fun buildExportFileName(title: String, timestamp: Long = System.currentTimeMillis()): String {
        val safeTitle = title
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim(' ', '.')
            .take(80)
            .ifBlank { "Paper" }
        val time = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestamp))
        return "Fe_Source_${safeTitle}_$time.zip"
    }

    internal fun buildLocalDirectoryExportFileName(timestamp: Long = System.currentTimeMillis()): String {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestamp))
        return "Fe_Local_Files_$time.zip"
    }

    internal fun buildCloudDirectoryExportFileName(timestamp: Long = System.currentTimeMillis()): String {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestamp))
        return "Fe_Cloud_Files_$time.zip"
    }

    internal fun localDirectoryExportBlockReason(
        fileCount: Int,
        totalBytes: Long,
        parsedPaperCount: Int
    ): LocalDirectoryExportBlockReason? = when {
        fileCount <= 0 -> LocalDirectoryExportBlockReason.EMPTY
        parsedPaperCount >= LOCAL_DIRECTORY_EXPORT_MAX_PARSED_PAPERS ->
            LocalDirectoryExportBlockReason.TOO_MANY_PARSED_PAPERS
        totalBytes > LOCAL_DIRECTORY_EXPORT_MAX_BYTES -> LocalDirectoryExportBlockReason.TOO_LARGE
        else -> null
    }

    internal fun resolveCloudUrl(baseUrl: String, url: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url.replaceFirst("http://", "https://")
        }
        val secureBase = baseUrl.replaceFirst("http://", "https://").trimEnd('/')
        return "$secureBase/${url.trimStart('/')}"
    }

    private fun inspectLocal(context: Context, source: PaperSource.Local): Inspection {
        val reader = ETS100FileReader.getReader(source.mode, context)
        val expected = source.resourceDirectoryNames.map { "$it/" }
        val missing = source.resourceDirectoryNames.flatMap { directoryName ->
            val path = "${ETS100FileReader.Path.getResourceDir()}/$directoryName"
            val expectedModified = source.resourceModifiedTimes[directoryName]?.takeIf { it > 0L }
            val actualModified = reader.getFileModifiedTime(path).takeIf { it > 0L }
            when {
                !ets100IsSafeName(directoryName) || !reader.exists(path) -> listOf("$directoryName/")
                reader.isSymbolicLink(path) -> listOf("$directoryName/（符号链接）")
                expectedModified != null && actualModified != null &&
                    kotlin.math.abs(actualModified - expectedModified) > 1_000L ->
                    listOf("$directoryName/（读取后已发生变化）")
                !reader.exists("$path/content.json") -> listOf("$directoryName/content.json")
                else -> inspectLocalDirectory(reader, path, "$directoryName/")
            }
        }
        return Inspection(
            expected = expected,
            missing = missing,
            relatedDataIndexes = ets100FindRelatedDataIndexes(reader, source.resourceDirectoryNames.toSet())
        )
    }

    private fun inspectLocalDirectory(
        reader: ETS100FileReader.Reader,
        sourcePath: String,
        relativePath: String
    ): List<String> {
        val children = reader.listFiles(sourcePath)
        if (children.isEmpty()) return listOf("$relativePath（目录为空或无法读取）")
        return children.flatMap { child ->
            val childPath = "$sourcePath/${child.name}"
            val childRelativePath = "$relativePath${child.name}"
            when {
                !ets100IsSafeName(child.name) -> listOf("$childRelativePath（非法文件名）")
                reader.isSymbolicLink(childPath) -> listOf("$childRelativePath（符号链接）")
                child.isDirectory -> inspectLocalDirectory(reader, childPath, "$childRelativePath/")
                else -> {
                    val readable = reader.openInputStream(childPath)?.use { true } ?: false
                    if (readable) emptyList() else listOf(childRelativePath)
                }
            }
        }
    }

    private fun inspectCloud(context: Context, source: PaperSource.Cloud): Inspection {
        val collisions = ets100CloudFileNameCollisions(source)
        val cacheDir = cloudCacheDirectory(context, source)
        val expected = source.contents.distinctBy { it.url }.map { cloudFileName(it.url).ifBlank { it.url } }
        val missing = source.contents.distinctBy { it.url }.mapNotNull { content ->
            val name = cloudFileName(content.url)
            name.takeIf {
                it.isBlank() || it in collisions || !isUsableZip(cloudCachedFile(cacheDir, content.url, it))
            }
                ?.ifBlank { content.url }
        }
        return Inspection(expected, missing, collisions.toList())
    }

    private fun stagePrivilegedLocalSources(
        inspection: LocalDirectoryInspection,
        stageDir: File
    ): Boolean {
        if (inspection.mode != ActivationMode.ROOT && inspection.mode != ActivationMode.SHIZUKU) return false
        val command = buildLocalSourceTarCommand(inspection)
        val input = when (inspection.mode) {
            ActivationMode.ROOT -> RootManager.openInputStreamAsRoot(command)
            ActivationMode.SHIZUKU -> ShizukuManager.openCommandInputStream(command)
        } ?: return false

        return runCatching {
            stageDir.deleteRecursively()
            check(stageDir.mkdirs() || stageDir.isDirectory) { "无法创建本地暂存目录" }
            val entryCount = input.use { extractTarToDirectory(it, stageDir) }
            check(entryCount > 0) { "批量复制未返回文件" }
            verifyStagedLocalSources(stageDir, inspection)
            true
        }.onFailure {
            Log.w(EXPORTER_TAG, "本地文件批量暂存失败，回退为逐文件读取", it)
            stageDir.deleteRecursively()
        }.getOrDefault(false)
    }

    private fun buildLocalSourceTarCommand(inspection: LocalDirectoryInspection): String {
        val entries = buildList {
            inspection.dataFiles.forEach { add(it.archivePath) }
            inspection.resourceDirectories.forEach { add("resource/${it.name}") }
        }
        check(entries.isNotEmpty()) { "没有可暂存的本地文件" }
        val base = shellQuoteForCommand(ETS100FileReader.Path.getETSSecondaryParent())
        val quotedEntries = entries.joinToString(" ") { shellQuoteForCommand(it) }
        return "tar -C $base -cf - $quotedEntries"
    }

    private fun shellQuoteForCommand(value: String): String =
        "'" + value.replace("'", "'\"'\"'") + "'"

    private fun verifyStagedLocalSources(stageDir: File, inspection: LocalDirectoryInspection) {
        inspection.dataFiles.forEach { file ->
            check(File(stageDir, file.archivePath).isFile) { "批量暂存缺少 ${file.archivePath}" }
        }
        inspection.resourceDirectories.forEach { directory ->
            check(File(stageDir, "resource/${directory.name}").isDirectory) {
                "批量暂存缺少 resource/${directory.name}/"
            }
        }
    }

    internal fun extractTarToDirectory(input: InputStream, destination: File): Int {
        val root = destination.canonicalFile
        var entryCount = 0
        val header = ByteArray(TAR_BLOCK_SIZE)
        while (readTarBlock(input, header)) {
            if (header.all { it == 0.toByte() }) break
            val name = tarText(header, 0, 100)
            val prefix = tarText(header, 345, 155)
            val path = listOf(prefix, name).filter { it.isNotBlank() }.joinToString("/")
            val size = tarOctal(header, 124, 12)
            val type = header[156].toInt().toChar()
            check(isSafeTarPath(path)) { "tar 包含不安全路径：$path" }
            val target = File(root, path).canonicalFile
            check(target.path == root.path || target.path.startsWith("${root.path}${File.separator}")) {
                "tar 路径越界：$path"
            }
            when (type) {
                '\u0000', '0' -> {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output -> copyTarBytes(input, output, size) }
                    skipTarPadding(input, size)
                    entryCount++
                }
                '5' -> {
                    target.mkdirs()
                    skipTarBytes(input, size)
                    skipTarPadding(input, size)
                    entryCount++
                }
                else -> {
                    skipTarBytes(input, size)
                    skipTarPadding(input, size)
                    error("tar 包含不支持的条目类型：$type")
                }
            }
        }
        return entryCount
    }

    internal fun extractResourceReferences(raw: String): Set<String> {
        val result = linkedSetOf<String>()
        val fieldPattern = Regex("""\"(?:fileName|url)\"\s*:\s*\"((?:\\.|[^\"\\])*)\"""")
        fieldPattern.findAll(raw).forEach { match ->
            val value = match.groupValues[1]
                .replace("\\/", "/")
                .replace("\\u002F", "/", ignoreCase = true)
            val name = value.substringAfterLast('/').substringBefore('?').removeSuffix(".zip")
            if (ets100IsSafeName(name)) result += name
        }
        return result
    }

    internal fun buildLocalDirectoryReadme(inspection: LocalDirectoryInspection): String = buildString {
        appendLine("Fe 本地题目文件导出")
        appendLine("本包仅用于向作者反馈无法解析或需要适配的题目，请勿作为常规答案分享包。")
        appendLine()
        appendLine("导出时间：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        appendLine("读取模式：${inspection.mode.title}")
        appendLine("源目录总大小：${formatExportSize(inspection.totalBytes)}")
        appendLine("当前已解析试卷：${inspection.parsedPaperCount} 套")
        if (inspection.isParsingInProgress) {
            appendLine("解析状态：后台解析尚未完成。以下结构仅包含导出时已完成的结果；未完成或无法解析的内容已作为原始文件包含在本包中。")
        }
        appendLine()
        appendLine("data 文件：")
        if (inspection.dataFiles.isEmpty()) appendLine("- 无")
        inspection.dataFiles.forEach { file ->
            appendLine("- ${file.archivePath} (${formatExportSize(file.size)})")
        }
        appendLine()
        appendLine("resource 内容：")
        if (inspection.resourceDirectories.isEmpty()) appendLine("- 无")
        val resourceGroups = inspection.resourceDirectories
            .associateBy { it.name }
            .keys
            .associateWith { mutableListOf<ETS100AnswerReader.Paper>() }
            .toMutableMap()
        inspection.parsedPapers.forEach { paper ->
            val source = paper.source as? PaperSource.Local ?: return@forEach
            source.resourceDirectoryNames.forEach { name -> resourceGroups[name]?.add(paper) }
        }
        val sharedResources = resourceGroups.filterValues { it.size > 1 }
        val groupedResources = resourceGroups.filterValues { it.size == 1 }
        inspection.parsedPapers.forEachIndexed { index, paper ->
            val resourceNames = groupedResources
                .filterValues { it.single() == paper }
                .keys
                .sorted()
            if (resourceNames.isNotEmpty()) {
                appendLine("已解析试卷 ${index + 1}：${paper.title}")
                resourceNames.forEach { appendLine("- resource/$it/") }
            }
        }
        if (sharedResources.isNotEmpty()) {
            appendLine("共享/多组引用资源：")
            sharedResources.toSortedMap().forEach { (name, papers) ->
                appendLine("- resource/$name/ -> ${papers.joinToString { it.title }}")
            }
        }
        val ungroupedResources = resourceGroups.filterValues { it.isEmpty() }.keys.sorted()
        if (ungroupedResources.isNotEmpty()) {
            appendLine("未解析或未分组资源：")
            ungroupedResources.forEach { appendLine("- resource/$it/") }
        }
        if (inspection.skipped.isNotEmpty()) {
            appendLine()
            appendLine("未导出的项目：")
            inspection.skipped.forEach { appendLine("- $it") }
        }
    }

    internal fun buildCloudDirectoryReadme(inspection: CloudDirectoryInspection): String = buildString {
        appendLine("Fe 云端缓存题目文件导出")
        appendLine("本包仅用于向作者反馈无法解析或需要适配的云端题目，请勿作为常规答案分享包。")
        appendLine()
        appendLine("导出时间：${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
        appendLine("缓存文件总大小：${formatExportSize(inspection.totalBytes)}")
        appendLine("当前已解析试卷：${inspection.parsedPaperCount} 套")
        if (inspection.isDownloadInProgress) {
            appendLine("下载状态：导出时仍有云端作业正在下载，缓存快照可能不完整。")
        }
        appendLine()
        appendLine("缓存文件：")
        inspection.files.forEach { appendLine("- ${it.archivePath} (${formatExportSize(it.size)})") }
        if (inspection.skipped.isNotEmpty()) {
            appendLine()
            appendLine("未导出的项目：")
            inspection.skipped.forEach { appendLine("- $it") }
        }
    }

    internal fun completedParsedPapers(
        papers: List<ETS100AnswerReader.Paper>
    ): List<ETS100AnswerReader.Paper> = papers.filter { paper ->
        paper.sections.none { it.category == "local_loading" } &&
            paper.sections.sumOf { it.questions.size } > 0
    }

    internal fun cloudFileName(url: String): String = ets100CloudFileName(url)

    fun isUsableZip(file: File): Boolean {
        if (!file.isFile || file.length() < 4) return false
        val hasZipMagic = runCatching {
            FileInputStream(file).use { input ->
                val magic = ByteArray(4)
                if (input.read(magic) != magic.size) return@use false
                val magicHex = magic.joinToString("") { "%02X".format(it) }
                magicHex == "504B0304" || magicHex == "504B0506" || magicHex == "504B0708"
            }
        }.getOrDefault(false)
        if (!hasZipMagic) return false

        if (ZipPasswordGenerator.hasValidEtsFooter(file)) return true

        return runCatching {
            ZipFile(file).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) entries.nextElement()
                true
            }
        }.getOrDefault(false)
    }

    private const val LOCAL_DIRECTORY_EXPORT_MAX_BYTES = 150L * 1024L * 1024L
    private const val LOCAL_DIRECTORY_EXPORT_MAX_PARSED_PAPERS = 3
    private const val CLOUD_DIRECTORY_EXPORT_MAX_FILES = 10_000
    private const val EXPORTER_TAG = "PaperSourceExporter"
}
