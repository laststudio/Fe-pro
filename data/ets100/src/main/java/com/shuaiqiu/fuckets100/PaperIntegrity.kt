package com.shuaiqiu.fuckets100

import org.json.JSONObject

/**
 * 卷面完整性对账结果 —— **缺题必须能被界面提示**。
 *
 * 为什么需要：卷面分来自官方 `set_score`，少几题时总分照样是 30/20 分，
 * 光看总分与"解析成功"根本发现不了漏题。解析器逐节比对后给出这份结果。
 */
data class PaperIntegrity(
    val isComplete: Boolean = true,
    val templateQuestions: Int = 0,
    val parsedQuestions: Int = 0,
    val missingVsTemplate: Int = 0,
    val sectionsBelowTemplate: List<String> = emptyList(),
    val sectionsNotCovered: List<String> = emptyList(),
    val missingPackages: Int = 0,
    val warnings: List<String> = emptyList(),
) {
    /** 给用户看的一句话说明；完整时返回 null。 */
    val notice: String?
        get() = when {
            isComplete -> null
            missingPackages > 0 ->
                "有 $missingPackages 个资源包未取到，本卷可能缺题（已取到 $parsedQuestions 题）"
            missingVsTemplate > 0 ->
                "本卷少 $missingVsTemplate 题（官方 $templateQuestions 题，已取到 $parsedQuestions 题）" +
                    sectionsBelowTemplate.takeIf { it.isNotEmpty() }?.let { "：${it.joinToString("；")}" }.orEmpty()
            sectionsNotCovered.isNotEmpty() ->
                "本卷有 ${sectionsNotCovered.size} 个大题未取到题目：${sectionsNotCovered.joinToString("；")}"
            else -> "本卷题目可能与官方试卷不完全一致"
        }

    companion object {
        /** 从解析器 `parse_homework_set` 的结果构造（缺字段时按"完整"处理，避免误报）。 */
        fun fromResolverResult(
            completeness: JSONObject?,
            coverage: JSONObject?,
            warnings: List<String>,
        ): PaperIntegrity? {
            if (completeness == null) return null
            fun strList(o: JSONObject?, key: String): List<String> =
                o?.optJSONArray(key)?.let { arr ->
                    (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
                } ?: emptyList()
            return PaperIntegrity(
                isComplete = completeness.optBoolean("is_complete", true),
                templateQuestions = completeness.optInt("template_questions", 0),
                parsedQuestions = completeness.optInt("parsed_questions", 0),
                missingVsTemplate = completeness.optInt("missing_vs_template", 0),
                sectionsBelowTemplate = strList(completeness, "sections_below_template"),
                sectionsNotCovered = strList(completeness, "sections_not_covered"),
                missingPackages = coverage?.optInt("missing_packages", 0) ?: 0,
                warnings = warnings.filter {
                    it.contains("below_template") || it.contains("above_template") ||
                        it.contains("missing_vs_homework") || it.contains("not_covered") ||
                        it.contains("family_fallback")
                },
            )
        }
    }
}
