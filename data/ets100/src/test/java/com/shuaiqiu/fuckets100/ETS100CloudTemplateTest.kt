package com.shuaiqiu.fuckets100

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 官方模板（res.json）链路的**纯函数**单测。
 *
 * 这里只测不依赖 JNI/网络的纯逻辑（响应解析、URL 拼装、合并入参组装）；
 * 真正调 `parse_homework_set` 的整卷合并由设备侧 instrumented test 覆盖。
 */
class ETS100CloudTemplateTest {

    private fun structBody(): JSONObject = JSONObject(
        """
        {"audio":[{"set_name":"2024至2025学年第一学期高二级英语听说能力监测A",
          "template":{"url":"42e2099edbef63c92deddb65463436b1.zip"},
          "contents":[
            {"code_id":"st1","group_name":"单词朗读","real_id":"9001","url":"a.zip"},
            {"code_id":"st2","group_name":"课内语篇朗读","real_id":"9002","url":"b.zip"},
            {"code_id":"st3","group_name":"课内语篇听读","real_id":"9003","url":"c.zip"},
            {"code_id":"st4","group_name":"课外语篇听读","real_id":"9004","url":"d.zip"},
            {"code_id":"st5","group_name":"故事复述","real_id":"9005","url":"e.zip"}]}]}
        """.trimIndent(),
    )

    @Test
    fun parseStructReadsTemplateAndMountingKeys() {
        val struct = ETS100CloudTemplate.parseStruct(structBody())!!
        assertEquals("2024至2025学年第一学期高二级英语听说能力监测A", struct.setName)
        assertEquals("42e2099edbef63c92deddb65463436b1.zip", struct.templateUrl)
        assertEquals(5, struct.contents.size)
        // real_id -> code_id：作业列表的 contents 只有 real_id，模板只认 code_id
        assertEquals("st3", struct.codeIdForRealId("9003"))
        assertEquals("", struct.codeIdForRealId("不存在"))
        assertEquals(listOf("st3"), struct.codeIdsForGroup("课内语篇听读"))
    }

    @Test
    fun parseStructHandlesMissingAudio() {
        assertNull(ETS100CloudTemplate.parseStruct(JSONObject("""{"body":{"audio":[]}}""")))
        assertNull(ETS100CloudTemplate.parseStruct(JSONObject("""{}""")))
    }

    @Test
    fun templateDownloadUrlIsIdempotent() {
        val cdn = "https://cdn.subject.ets100.com/made/"
        assertEquals(cdn + "abc.zip", ETS100CloudTemplate.templateDownloadUrl("abc.zip"))
        assertEquals(cdn + "abc.zip", ETS100CloudTemplate.templateDownloadUrl("abc"))
        assertEquals(cdn + "abc.zip", ETS100CloudTemplate.templateDownloadUrl("/abc.zip"))
        // 已经是完整 URL 时原样保留（并规范为 https）
        assertEquals(cdn + "abc.zip", ETS100CloudTemplate.templateDownloadUrl(cdn + "abc.zip"))
        assertEquals(cdn + "abc.zip", ETS100CloudTemplate.templateDownloadUrl("http://cdn.subject.ets100.com/made/abc.zip"))
        assertEquals("", ETS100CloudTemplate.templateDownloadUrl(""))
    }

    @Test
    fun buildPackagesCarriesMountingKeysAndOrder() {
        val arr = ETS100CloudTemplate.buildPackages(
            listOf(
                ETS100CloudTemplate.PackageQuestions(
                    structureType = "collector.read",
                    groupName = "单词朗读",
                    questions = JSONArray().put(JSONObject().put("question", "Q1").put("answers", JSONArray().put("A1"))),
                    codeId = "st1",
                    realId = "9001",
                    order = 0,
                ),
                ETS100CloudTemplate.PackageQuestions(
                    structureType = "collector.picture",
                    groupName = "故事复述",
                    questions = JSONArray().put(JSONObject().put("question", "Q2")),
                    codeId = "st5",
                    realId = "9005",
                    order = 4,
                ),
            ),
        )
        assertEquals(2, arr.length())
        val first = arr.getJSONObject(0)
        assertEquals("collector.read", first.optString("structure_type"))
        assertEquals("单词朗读", first.optString("group_name"))
        assertEquals("st1", first.optString("code_id"))
        assertEquals("9001", first.optString("real_id"))
        assertEquals(0, first.optInt("order"))
        assertEquals(1, first.optJSONArray("questions")!!.length())
        // 未标注 order 时按数组下标兜底
        val auto = ETS100CloudTemplate.buildPackages(
            listOf(
                ETS100CloudTemplate.PackageQuestions("collector.read", "a", JSONArray()),
                ETS100CloudTemplate.PackageQuestions("collector.read", "b", JSONArray()),
            ),
        )
        assertEquals(0, auto.getJSONObject(0).optInt("order"))
        assertEquals(1, auto.getJSONObject(1).optInt("order"))
    }

    @Test
    fun realGuangdongStructFixtureParses() {
        // 真实 /m/homework/struct 响应片段（广东高中听说能力监测 20 分卷）
        val real = JSONObject(
            """
            {"audio":[{"set_name":"2024至2025学年第一学期高二级英语听说能力监测A",
              "template":{"url":"42e2099edbef63c92deddb65463436b1.zip","name":"听说能力监测A"},
              "contents":[{"code_id":"st1","group_name":"单词朗读","real_id":"10101","url":"x.zip","order":1},
                          {"code_id":"st2","group_name":"课内语篇朗读","real_id":"10102","url":"y.zip","order":2}]}]}
            """.trimIndent(),
        )
        val struct = ETS100CloudTemplate.parseStruct(real)!!
        assertTrue(struct.setName.contains("听说能力监测"))
        assertEquals("st2", struct.codeIdForRealId("10102"))
    }
}

class PaperIntegrityTest {

    private fun result(
        isComplete: Boolean,
        missing: Int = 0,
        below: List<String> = emptyList(),
        notCovered: List<String> = emptyList(),
        missingPackages: Int = 0,
    ): ETS100AnswerReader.PaperIntegrity? = ETS100AnswerReader.PaperIntegrity.fromResolverResult(
        completeness = JSONObject()
            .put("is_complete", isComplete)
            .put("template_questions", 14)
            .put("parsed_questions", 14 - missing)
            .put("missing_vs_template", missing)
            .put("sections_below_template", JSONArray(below))
            .put("sections_not_covered", JSONArray(notCovered)),
        coverage = JSONObject().put("missing_packages", missingPackages),
        warnings = emptyList(),
    )

    @Test
    fun completePaperHasNoNotice() {
        val integrity = result(isComplete = true)!!
        assertTrue(integrity.isComplete)
        assertNull("完整卷不应给出提示", integrity.notice)
    }

    @Test
    fun missingQuestionsProduceActionableNotice() {
        val integrity = result(isComplete = false, missing = 2, below = listOf("回答问题: 2 题（官方模板 4 题）"))!!
        assertFalse(integrity.isComplete)
        val notice = integrity.notice!!
        assertTrue("提示应说明少了几题", notice.contains("少 2 题"))
        assertTrue("提示应点名是哪一节", notice.contains("回答问题"))
    }

    @Test
    fun uncoveredSectionIsReported() {
        val integrity = result(isComplete = false, notCovered = listOf("故事复述"))!!
        assertTrue(integrity.notice!!.contains("故事复述"))
    }

    @Test
    fun missingPackagesReportedFirst() {
        // 有包没取到时优先提示"包"（更接近根因）
        val integrity = result(isComplete = false, missingPackages = 3)!!
        assertTrue(integrity.notice!!.contains("3 个资源包"))
    }

    @Test
    fun absentCompletenessMeansUnknownNotIncomplete() {
        // 旧解析器 / 无官方模板时不该误报
        val integrity = ETS100AnswerReader.PaperIntegrity.fromResolverResult(null, null, emptyList())
        assertNull(integrity)
    }

    @Test
    fun resolverWarningsAreFilteredToIntegrityRelated() {
        val integrity = ETS100AnswerReader.PaperIntegrity.fromResolverResult(
            completeness = JSONObject().put("is_complete", false).put("missing_vs_template", 1),
            coverage = JSONObject(),
            warnings = listOf(
                "question_count_below_template: 少 1 题",
                "partial_template_coverage:3/7",
                "package_mounted_by_family_fallback: 听选信息1",
                "无关噪音",
            ),
        )!!
        assertEquals(2, integrity.warnings.size)
        assertTrue(integrity.warnings.any { it.contains("below_template") })
        assertTrue(integrity.warnings.any { it.contains("family_fallback") })
    }
}
