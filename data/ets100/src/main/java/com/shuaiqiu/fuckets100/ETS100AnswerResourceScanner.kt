package com.shuaiqiu.fuckets100

import android.util.Log
import org.json.JSONObject


/**
 * resource/ 目录扫描与试卷分组实现。
 *
 * 从 [ETS100AnswerReader] 中抽出，保留原有扫描缓存语义。
 */
internal object ETS100AnswerResourceScanner {
    private const val TAG = "ETS100AnswerReader"
    private const val RESOURCE_SCAN_CACHE_TTL_MS = 15_000L

    private val debugTimeFormatter = java.text.SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss.SSS",
        java.util.Locale.getDefault()
    )

    @Volatile
    private var lastResourceScanCache: ResourceScanCache? = null

    fun clearCache(mode: ActivationMode? = null) {
        val cache = lastResourceScanCache ?: return
        if (mode == null || cache.mode == mode) {
            Log.d(TAG, "clearResourceScanCache: clear mode=${cache.mode}")
            lastResourceScanCache = null
        }
    }

    internal fun scanResourceFolders(reader: ETS100FileReader.Reader): List<ETS100FileReader.FileItem> {
        val mode = reader.getMode()
        val now = System.currentTimeMillis()
        lastResourceScanCache?.let { cache ->
            if (cache.mode == mode && now - cache.createdAtMs <= RESOURCE_SCAN_CACHE_TTL_MS) {
                Log.d(TAG, "scanResourceFolders: 使用缓存 mode=$mode, count=${cache.folders.size}")
                return cache.folders
            }
        }

        val resourceDir = ETS100FileReader.Path.getResourceDir()
        val contentFolders = if (reader is ETS100FileReader.ContentFolderReader) {
            reader.listContentFolders(resourceDir)
        } else {
            reader.listFiles(resourceDir)
                .filter { it.isDirectory }
                .mapNotNull { item ->
                    val folderPath = "$resourceDir/${item.name}"
                    val contentJsonPath = "$folderPath/content.json"

                    if (reader.getFileSize(contentJsonPath) <= 0) {
                        return@mapNotNull null
                    }

                    val lastModified = item.lastModified.takeIf { it > 0L }
                        ?: reader.getFileModifiedTime(folderPath)

                    item.copy(lastModified = lastModified)
                }
        }

        val sortedFolders = contentFolders
            .sortedWith(
                compareByDescending<ETS100FileReader.FileItem> { it.lastModified }
                    .thenByDescending { it.name }
            )

        Log.d(TAG, "scanResourceFolders: 找到 ${sortedFolders.size} 个含 content.json 的文件夹")
        lastResourceScanCache = ResourceScanCache(mode, now, sortedFolders)
        sortedFolders.forEachIndexed { index, folder ->
            val readableTime = if (folder.lastModified > 0L) {
                debugTimeFormatter.format(java.util.Date(folder.lastModified))
            } else {
                "0/无效时间"
            }
            Log.d(
                TAG,
                "scanResourceFolders: #${index + 1} name=${folder.name}, " +
                    "lastModified=${folder.lastModified}, time=$readableTime, " +
                    "contentJsonSize=${folder.size}"
            )
        }
        return sortedFolders
    }

    internal fun groupResourceFolders(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>
    ): List<List<ETS100FileReader.FileItem>> {
        if (folders.isEmpty()) return emptyList()
        val hasReliableTime = folders.any { it.lastModified > 0L } &&
            folders.map { it.lastModified }.distinct().size > 1
        val hasMillisecondPrecision = folders.any { it.lastModified % 1000L != 0L }
        Log.d(
            TAG,
            "groupResourceFolders: count=${folders.size}, " +
                "hasReliableTime=$hasReliableTime, hasMillisecondPrecision=$hasMillisecondPrecision"
        )

        if (hasReliableTime) {
            return groupByTime(folders, thresholdMs = 2000L)
        }

        Log.w(TAG, "groupResourceFolders: resource 修改时间不可靠，按 content.json 结构兜底拆分")
        return groupByKnownPaperFolderCounts(reader, folders)
    }

    internal fun groupResourceFoldersFast(
        folders: List<ETS100FileReader.FileItem>
    ): List<List<ETS100FileReader.FileItem>> {
        if (folders.isEmpty()) return emptyList()
        val hasReliableTime = folders.any { it.lastModified > 0L } &&
            folders.map { it.lastModified }.distinct().size > 1

        if (hasReliableTime) {
            return groupByTime(folders, thresholdMs = 2000L)
        }

        val groupSizes = listOf(13, 10, 7, 3)
        val groups = mutableListOf<List<ETS100FileReader.FileItem>>()
        var index = 0
        while (index < folders.size) {
            val remaining = folders.size - index
            val size = groupSizes
                .firstOrNull { remaining >= it && canSplitByKnownGroupSizes(remaining - it, groupSizes) }
                ?: groupSizes.firstOrNull { remaining >= it && (remaining == it || (remaining - it) >= 3) }
                ?: remaining.coerceAtMost(13)
            groups.add(folders.subList(index, index + size))
            index += size
        }

        Log.d(TAG, "groupResourceFoldersFast: ${folders.size} 个文件夹快速拆成 ${groups.size} 组")
        return groups
    }

    internal fun canSplitByKnownGroupSizes(
        count: Int,
        groupSizes: List<Int>
    ): Boolean {
        if (count == 0) return true
        if (count < (groupSizes.minOrNull() ?: 0)) return false
        return groupSizes.any { count >= it && canSplitByKnownGroupSizes(count - it, groupSizes) }
    }

    internal fun groupByKnownPaperFolderCounts(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>
    ): List<List<ETS100FileReader.FileItem>> {
        val groupSizes = listOf(13, 10, 7, 3)
        val groups = mutableListOf<List<ETS100FileReader.FileItem>>()
        var index = 0

        while (index < folders.size) {
            val remaining = folders.size - index
            val candidateSizes = groupSizes
                .filter { remaining >= it && (remaining == it || (remaining - it) >= 3) }
                .ifEmpty { listOf(remaining.coerceAtMost(13)) }
            val size = candidateSizes
                .map { candidateSize ->
                    val candidate = folders.subList(index, index + candidateSize)
                    val baseScore = ETS100AnswerReader.scoreResourceGroupCandidate(reader, candidate)
                    val exactGuangdongJuniorBonus = if (
                        remaining >= 10 &&
                        candidateSize == 7 &&
                        ETS100AnswerReader.detectResourceGroupTemplate(reader, candidate) == ResourceGroupTemplate.GUANGDONG_JUNIOR
                    ) {
                        30
                    } else {
                        0
                    }
                    candidateSize to (baseScore + exactGuangdongJuniorBonus)
                }
                .maxWithOrNull(
                    compareBy<Pair<Int, Int>> { it.second }
                        .thenByDescending { it.first }
                )
                ?.first
                ?: remaining.coerceAtMost(13)
            groups.add(folders.subList(index, index + size))
            index += size
        }

        Log.d(TAG, "groupByKnownPaperFolderCounts: ${folders.size} 个文件夹按结构兜底拆成 ${groups.size} 组")
        return groups
    }

    internal fun buildResourceOrderMap(reader: ETS100FileReader.Reader): Map<String, Int> {
        val dataDirPath = ETS100FileReader.Path.getDataDir()
        val dataFiles = reader.listFiles(dataDirPath)
            .filter { !it.isDirectory && it.size > ETS100FileReader.Path.MIN_FILE_SIZE }
            .sortedByDescending { it.lastModified }

        val orderMap = linkedMapOf<String, Int>()
        var order = 0

        for (dataFile in dataFiles) {
            val content = reader.readFile("$dataDirPath/${dataFile.name}") ?: continue
            val json = runCatching { JSONObject(content) }.getOrNull() ?: continue
            val structureType = json.optString("structure_type", "")
            val sectionData = json.optJSONArray("sectionData") ?: continue

            for (sectionIndex in 0 until sectionData.length()) {
                val section = sectionData.optJSONObject(sectionIndex) ?: continue
                val sectionItemData = section.optJSONArray("sectionItemData") ?: continue

                for (itemIndex in 0 until sectionItemData.length()) {
                    val item = sectionItemData.optJSONObject(itemIndex) ?: continue
                    val fileName = item.optString("fileName", "")
                    if (fileName.isNotEmpty() && !orderMap.containsKey(fileName)) {
                        orderMap[fileName] = order++
                    }
                }
            }
        }

        Log.d(TAG, "buildResourceOrderMap: 建立 ${orderMap.size} 个 resource 顺序映射")
        return orderMap
    }

    internal fun orderResourceFoldersByPaperStructure(
        folders: List<ETS100FileReader.FileItem>,
        resourceOrderMap: Map<String, Int>
    ): List<ETS100FileReader.FileItem> {
        if (resourceOrderMap.isEmpty()) return folders

        return folders.withIndex()
            .sortedWith(
                compareBy<IndexedValue<ETS100FileReader.FileItem>> {
                    resourceOrderMap[it.value.name] ?: Int.MAX_VALUE
                }.thenBy { it.index }
            )
            .map { it.value }
    }

    /**
     * Step 2: 按时间分组算法
     * 相邻文件夹修改时间间隔 ≤ thresholdMs → 同一组
     */
    internal fun groupByTime(
        folders: List<ETS100FileReader.FileItem>,
        thresholdMs: Long = 2000
    ): List<List<ETS100FileReader.FileItem>> {
        if (folders.isEmpty()) return emptyList()

        Log.d(TAG, "╔═══ groupByTime 分组日志 ═══")
        Log.d(TAG, "║ 阈值: ${thresholdMs}ms (${thresholdMs/1000.0}秒)")
        Log.d(TAG, "║ 原始文件夹数量: ${folders.size}")

        val groups = mutableListOf<List<ETS100FileReader.FileItem>>()
        var currentGroup = mutableListOf(folders[0])
        var groupIndex = 0

        for (i in 1 until folders.size) {
            val prev = folders[i - 1]
            val curr = folders[i]
            val timeDiff = curr.lastModified - prev.lastModified
            // 宝贝注意：因为是降序排列（最新在前），timeDiff 应该是负数
            // 同组条件：时间差的绝对值 <= 阈值（即时间接近）
            val isSameGroup = kotlin.math.abs(timeDiff) <= thresholdMs

            Log.d(TAG, "║ [$i] ${curr.name.take(12)}: 时间差=${timeDiff}ms (${timeDiff/1000.0}秒), |差值|=${kotlin.math.abs(timeDiff)}ms, ${if (isSameGroup) "同组" else "新组"}")

            if (isSameGroup) {
                currentGroup.add(curr)
            } else {
                groupIndex++
                groups.add(currentGroup)
                currentGroup = mutableListOf(curr)
                Log.d(TAG, "║     → 创建新组 #$groupIndex")
            }
        }
        groups.add(currentGroup)

        // 输出每组的统计
        Log.d(TAG, "║ ─── 分组结果 ───")
        groups.forEachIndexed { idx, group ->
            val folderNames = group.joinToString(", ") { it.name.take(8) }
            Log.d(TAG, "║ 组 #$idx: ${group.size}个文件夹 [$folderNames]")
        }
        Log.d(TAG, "║ 总计: ${groups.size} 组")
        Log.d(TAG, "╚═══ groupByTime 结束 ═══")

        return groups
    }
}
