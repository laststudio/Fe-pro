package com.shuaiqiu.fuckets100

import android.util.Log
import org.json.JSONObject


/**
 * data/ 与 resource/ 普通试卷的解析实现。
 *
 * 从 [ETS100AnswerReader] 中抽出，保持既有解析行为不变。
 */
internal object ETS100AnswerPaperParser {
    private const val TAG = "ETS100AnswerReader"

    internal fun parsePaper(reader: ETS100FileReader.Reader, dataFile: ETS100FileReader.FileItem, processedPaths: MutableSet<String>): ETS100AnswerReader.Paper? {
        val dataFilePath = "${ETS100FileReader.Path.getDataDir()}/${dataFile.name}"
        Log.d(TAG, "parsePaper: 解析试卷文件 ${dataFile.name}")

        val content = reader.readFile(dataFilePath)
        if (content == null) {
            Log.w(TAG, "parsePaper: 无法读取 data 文件: ${dataFile.name}")
            return null
        }

        return try {
            val json = JSONObject(content)

            // 喵~ 判断文件类型：常规习题 or 习题组
            val hasSectionData = json.has("sectionData")
            val hasDataArray = json.has("data")
            val newStruct = json.optInt("new_struct", 0)

            Log.d(TAG, "parsePaper: hasSectionData=$hasSectionData, hasDataArray=$hasDataArray, newStruct=$newStruct")

            return when {
                hasSectionData -> {
                    // 常规习题（旧版格式）
                    Log.d(TAG, "parsePaper: 检测到常规习题格式")
                    parseNormalExercise(reader, json, dataFile, processedPaths)
                }
                hasDataArray && newStruct == 1 -> {
                    // 习题组（新版格式）
                    Log.d(TAG, "parsePaper: 检测到习题组格式")
                    parseExerciseGroup(reader, json, dataFile)
                }
                else -> {
                    Log.w(TAG, "parsePaper: 无法识别的数据格式: ${dataFile.name}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "parsePaper: Failed to parse JSON from ${dataFile.name}: ${e.message}", e)
            null
        }
    }

    /**
     * 解析常规习题（sectionData 格式）
     * 喵~ 这是原来的 parsePaper 逻辑，现在独立出来喵！
     * @param processedPaths 用于去重，跳过Step4中已处理的content.json路径
     */
    internal fun parseNormalExercise(reader: ETS100FileReader.Reader, json: JSONObject, dataFile: ETS100FileReader.FileItem, processedPaths: MutableSet<String>): ETS100AnswerReader.Paper? {
        val paperId = json.optLong("paperId", 0L)
        val sectionData = json.optJSONArray("sectionData")

        Log.d(TAG, "parseNormalExercise: paperId=$paperId, sectionData=${sectionData?.length()}")

        if (paperId == 0L || sectionData == null) {
            Log.w(TAG, "parseNormalExercise: Invalid paper data: paperId=$paperId, sectionData=${sectionData == null}")
            return null
        }

        // 解析每个section
        val sections = mutableListOf<ETS100AnswerReader.Section>()
        var globalQuestionIndex = 0

        // 遍历sectionData
        for (sectionIndex in 0 until sectionData.length()) {
            val section = sectionData.getJSONObject(sectionIndex)
            val caption = section.optString("caption", "")
            val category = section.optString("category", "")
            val typeName = getTypeName(category)
            val sectionItemData = section.optJSONArray("sectionItemData")

            Log.d(TAG, "parseNormalExercise: section $sectionIndex (caption=$caption, category=$category)")

            if (sectionItemData == null) {
                Log.w(TAG, "parseNormalExercise: section $sectionIndex 缺少 sectionItemData")
                continue
            }

            // 遍历sectionItemData，跳过mainItem
            val processedFileNames = mutableSetOf<String>()
            val sectionQuestions = mutableListOf<ETS100AnswerReader.Question>()
            var sectionOriginalContent: String? = null

            for (itemIndex in 0 until sectionItemData.length()) {
                val item = sectionItemData.getJSONObject(itemIndex)

                // 跳过mainItem=true的项
                val mainItem = item.optBoolean("mainItem", false)
                if (mainItem) {
                    Log.d(TAG, "parseNormalExercise: section $sectionIndex, item $itemIndex 跳过mainItem=true")
                    continue
                }

                val fileName = item.optString("fileName", "")
                if (fileName.isEmpty()) {
                    continue
                }

                // 跳过已处理的文件名（同一section内的重复）
                if (fileName in processedFileNames) {
                    Log.d(TAG, "parseNormalExercise: section $sectionIndex, item $itemIndex 跳过重复fileName=$fileName")
                    continue
                }
                processedFileNames.add(fileName)

                // 跳过Step4中已处理的content.json（跨section去重）
                val contentPath = "${ETS100FileReader.Path.getResourceDir()}/$fileName/content.json"
                if (processedPaths.contains(contentPath)) {
                    Log.d(TAG, "parseNormalExercise: section $sectionIndex, item $itemIndex 跳过已处理路径=$contentPath")
                    continue
                }
                processedPaths.add(contentPath)

                Log.d(TAG, "parseNormalExercise: 处理文件 fileName=$fileName")

                // 读取并解析content.json获取题目
                val (questions, originalContent) = readAndParseContent(reader, fileName, globalQuestionIndex, category, typeName, caption)

                if (originalContent != null && sectionOriginalContent == null) {
                    sectionOriginalContent = originalContent
                }

                for (q in questions) {
                    sectionQuestions.add(q)
                    globalQuestionIndex++
                }
            }

            if (sectionQuestions.isNotEmpty()) {
                sections.add(ETS100AnswerReader.Section(
                    caption = caption,
                    category = category,
                    typeName = typeName,
                    questions = sortQuestionsByOfficialOrder(sectionQuestions),
                    originalContent = sectionOriginalContent
                ))
            }
        }

        Log.d(TAG, "parseNormalExercise: 完成 ${dataFile.name} 包含 ${sections.size} 个section共${globalQuestionIndex}道题目")

        val regionLabel = "未知"
        val paperName: String? = null

        return ETS100AnswerReader.Paper(
            paperId = paperId,
            title = "试卷 #$paperId",
            dataFileName = dataFile.name,
            fileSize = dataFile.size,
            sections = sections,
            downloadTime = dataFile.lastModified,
            regionLabel = regionLabel,
            paperName = paperName
        )
    }

    /**
     * 解析习题组（data[] + struct.contents 格式）
     * 喵~ 这是新版习题组格式，通过 data[] 数组组织题型喵！
     *
     * 习题组结构：
     * - base_url: CDN 资源基础 URL
     * - data[]: 题目组数组
     *   - name: 题目名称
     *   - column: 题目分类
     *   - struct.contents[]: 题型内容数组（st1-st7）
     *     - code_id: 题型代码
     *     - group_name: 题型名称
     *     - audio_url: 音频完整 URL
     *     - url: 资源包名称（需结合 base_url 下载）
     */
    internal fun parseExerciseGroup(reader: ETS100FileReader.Reader, json: JSONObject, dataFile: ETS100FileReader.FileItem): ETS100AnswerReader.Paper? {
        val baseUrl = json.optString("base_url", "")
        val dataArray = json.optJSONArray("data")

        if (dataArray == null || dataArray.length() == 0) {
            Log.w(TAG, "parseExerciseGroup: data 数组为空")
            return null
        }

        Log.d(TAG, "parseExerciseGroup: baseUrl=$baseUrl, dataCount=${dataArray.length()}")

        val sections = mutableListOf<ETS100AnswerReader.Section>()
        var globalQuestionIndex = 0

        // 喵~ 遍历每个题目组
        for (groupIndex in 0 until dataArray.length()) {
            val group = dataArray.getJSONObject(groupIndex)
            val groupName = group.optString("name", "")
            val groupColumn = group.optString("column", "")
            val groupId = group.optString("id", "")

            Log.d(TAG, "parseExerciseGroup: group $groupIndex (name=$groupName, column=$groupColumn, id=$groupId)")

            val struct = group.optJSONObject("struct")
            if (struct == null) {
                Log.w(TAG, "parseExerciseGroup: group $groupIndex 缺少 struct")
                continue
            }

            val contents = struct.optJSONArray("contents")
            if (contents == null || contents.length() == 0) {
                Log.w(TAG, "parseExerciseGroup: group $groupIndex 缺少 contents")
                continue
            }

            // 喵~ 遍历每个题型 (st1-st7)
            for (contentIndex in 0 until contents.length()) {
                val content = contents.getJSONObject(contentIndex)

                val codeId = content.optString("code_id", "")
                val groupNameType = content.optString("group_name", "")
                val audioUrl = content.optString("audio_url", "")
                val resourceUrl = content.optString("url", "")
                val realId = content.optString("real_id", "")

                Log.d(TAG, "parseExerciseGroup: content $contentIndex (codeId=$codeId, groupName=$groupNameType, url=$resourceUrl)")

                // 喵~ 根据 code_id 获取 category
                val category = ETS100AnswerReader.exerciseGroupCodeIdToCategory[codeId] ?: "unknown"
                val typeName = ETS100AnswerReader.exerciseGroupCodeIdMap[codeId] ?: groupNameType

                // 喵~ 尝试从 resource 目录读取 content.json（如果已下载）
                // resourceUrl 是 zip 文件名如 "3ec29bc4bb3a10d089ee6fb0a66c259b.zip"
                // resource 目录名是去掉 .zip 后缀的文件名
                val resourceDirName = resourceUrl.removeSuffix(".zip")
                val (questions, originalContent) = readAndParseContent(
                    reader, resourceDirName, globalQuestionIndex, category, typeName, groupName
                )

                if (questions.isNotEmpty()) {
                    sections.add(ETS100AnswerReader.Section(
                        caption = groupName,
                        category = category,
                        typeName = typeName,
                        questions = sortQuestionsByOfficialOrder(questions),
                        originalContent = originalContent
                    ))
                    globalQuestionIndex += questions.size
                } else {
                    // 喵~ 如果 resource 目录没有 content.json，创建一个占位题目
                    Log.d(TAG, "parseExerciseGroup: 无法读取 resource，创建占位题目 codeId=$codeId")
                    sections.add(ETS100AnswerReader.Section(
                        caption = groupName,
                        category = category,
                        typeName = typeName,
                        questions = listOf(ETS100AnswerReader.Question(
                            order = globalQuestionIndex + 1,
                            sectionOrder = 1,
                            sectionCaption = groupName,
                            typeName = typeName,
                            questionText = "[$typeName] $groupName",
                            answers = emptyList(),
                            originalText = null,
                            category = category
                        )),
                        originalContent = null
                    ))
                    globalQuestionIndex++
                }
            }
        }

        Log.d(TAG, "parseExerciseGroup: 完成 ${dataFile.name} 包含 ${sections.size} 个section共${globalQuestionIndex}道题目")

        // 喵~ 习题组没有 paperId，使用文件名作为标识
        val paperId = dataFile.name.hashCode().toLong().let { if (it < 0) -it else it }

        return ETS100AnswerReader.Paper(
            paperId = paperId,
            title = dataArray.getJSONObject(0).optString("name", "习题组 #${dataFile.name}"),
            dataFileName = dataFile.name,
            fileSize = dataFile.size,
            sections = sections,
            downloadTime = dataFile.lastModified,
            regionLabel = "习题组",
            paperName = dataArray.getJSONObject(0).optString("name", "").ifEmpty { null }
        )
    }

    /**
     * 读取并解析content.json
     *
     * @return Pair(questions, originalContent)
     */
    internal fun readAndParseContent(
        reader: ETS100FileReader.Reader,
        fileName: String,
        startIndex: Int,
        category: String,
        typeName: String,
        sectionCaption: String
    ): Pair<List<ETS100AnswerReader.Question>, String?> {
        val resourceDirPath = "${ETS100FileReader.Path.getResourceDir()}/$fileName"
        val contentFilePath = "$resourceDirPath/content.json"

        Log.d(TAG, "readAndParseContent: 读取 $contentFilePath")

        val content = reader.readFile(contentFilePath)
        if (content == null) {
            Log.w(TAG, "readAndParseContent: 无法读取content.json: $contentFilePath")
            return Pair(emptyList(), null)
        }

        return try {
            val json = JSONObject(content)
            ETS100AnswerReader.parseContentJson(
                json = json,
                startIndex = startIndex,
                category = category,
                typeName = typeName,
                sectionCaption = sectionCaption,
                infoJson = reader.readFile("$resourceDirPath/info.json").orEmpty(),
                content2Json = reader.readFile("$resourceDirPath/content2.json").orEmpty(),
            )
        } catch (e: ResolverAuthorizationDeniedException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "readAndParseContent: Failed to parse: ${e.message}", e)
            Pair(emptyList(), null)
        }
    }

    /**
     * 通过 libresolver.so 解析 content.json。Kotlin 只负责把 Native 结果映射为
     * Fe 的 UI 数据模型，不再从 ETS JSON 中提取题目、答案或选项。
     */
    internal fun parseGuangdongHighPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>
    ): List<ETS100AnswerReader.Paper> = ETS100AnswerLocalTemplate.parseLocalResourcePapers(
        reader,
        folders,
        groupIndex,
        totalGroups,
        processedPaths,
        LocalPaperKind.GUANGDONG_HIGH
    )

    internal fun parseGuangdongJuniorPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>
    ): List<ETS100AnswerReader.Paper> = ETS100AnswerLocalTemplate.parseLocalResourcePapers(
        reader,
        folders,
        groupIndex,
        totalGroups,
        processedPaths,
        LocalPaperKind.GUANGDONG_JUNIOR
    )

    internal fun parseBeijingJuniorPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>
    ): List<ETS100AnswerReader.Paper> = ETS100AnswerLocalTemplate.parseLocalResourcePapers(
        reader,
        folders,
        groupIndex,
        totalGroups,
        processedPaths,
        LocalPaperKind.BEIJING_JUNIOR
    )

    internal fun parseBeijingHighPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>
    ): List<ETS100AnswerReader.Paper> = ETS100AnswerLocalTemplate.parseLocalResourcePapers(
        reader,
        folders,
        groupIndex,
        totalGroups,
        processedPaths,
        LocalPaperKind.BEIJING_HIGH
    )

    internal fun parseGenericPapers(
        reader: ETS100FileReader.Reader,
        folders: List<ETS100FileReader.FileItem>,
        groupIndex: Int,
        totalGroups: Int,
        processedPaths: MutableSet<String>
    ): List<ETS100AnswerReader.Paper> = ETS100AnswerLocalTemplate.parseLocalResourcePapers(
        reader,
        folders,
        groupIndex,
        totalGroups,
        processedPaths,
        LocalPaperKind.GENERIC
    )

    internal fun orderLocalSections(
        sections: List<ETS100AnswerReader.Section>,
        sectionOrder: List<String>
    ): List<ETS100AnswerReader.Section> {
        if (sectionOrder.isEmpty()) return sections
        val orderMap = sectionOrder.withIndex().associate { it.value to it.index }
        return sections.withIndex()
            .sortedWith(
                compareBy<IndexedValue<ETS100AnswerReader.Section>> {
                    orderMap[it.value.typeName] ?: orderMap[it.value.caption] ?: Int.MAX_VALUE
                }.thenBy { it.index }
            )
            .map { it.value }
    }

    /**
     * Step 5: 扫描 data/ 目录获取常规习题
     * 解析 sectionData → fileName → content.json
     * 跳过 Step 4 中已处理的 content.json（去重）
     */
}
