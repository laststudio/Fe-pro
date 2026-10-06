package com.shuaiqiu.fuckets100

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 设备侧 JNI / 契约冒烟测试（真机或模拟器）。
 *
 * 与仓库内 `tools/jni_probe/run.sh`（宿主侧探针）互补：
 *   * 宿主侧证明 JNI 注册 + 方法体 + 契约信封；
 *   * 本测试额外覆盖 **bionic / Android 运行时 / APK 打包 abi / Kotlin 声明是否漂移**，
 *     并且真的在应用进程里跑 —— 即 `System.loadLibrary("resolver")` 那条路径。
 *
 * 运行：
 *   ./gradlew :app:connectedDebugAndroidTest
 * 或只跑本类：
 *   ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.shuaiqiu.fuckets100.ResolverJniInstrumentedTest
 *
 * 前置：`app/src/main/jniLibs/arm64-v8a/` 里必须是当前 release 的 libresolver.so
 * （可用 `.github/actions/fetch-latest-resolver` 的逻辑或手动放入）。
 */
@RunWith(AndroidJUnit4::class)
class ResolverJniInstrumentedTest {

    private fun loadResolverNative() {
        // 触发 ResolverNative 的 init { System.loadLibrary("resolver") }
        ResolverNative.isAuthEnabled()
    }

    @Test
    fun jniLibraryLoadsAndRegistersAllMethods() {
        loadResolverNative()
        // 能调用到这里说明：so 已打包进 apk、JNI_OnLoad 成功、动态注册的方法名与签名全部匹配
        val caps = ResolverContract.capabilities()
        assertTrue("capabilities 应成功: ${caps.errorCode} ${caps.errorMessage}", caps.ok)
        val ops = caps.result?.optJSONArray("ops")
        assertNotNull("capabilities 应返回 ops", ops)
        assertTrue("ops 数量应 >= 13（实际 ${ops!!.length()}）", ops.length() >= 13)
    }

    @Test
    fun integrityCheckPassesOnDevice() {
        val report = JSONObject(ResolverNative.selfIntegrityCheck())
        // 应用进程名以 com.shuaiqiu.fuckets100 开头 -> cmdline 校验通过
        assertTrue(
            "cmdline 校验应通过（真机断言包名与进程名一致）: $report",
            report.optBoolean("cmdline_valid", false),
        )
        assertFalse("不应被判定为调试中: $report", report.optBoolean("debugged", false))
        assertFalse("不应检出 hook: $report", report.optBoolean("hook_detected", false))
    }

    /**
     * 可执行段自校验在**真机**上是否成立。
     *
     * 这是"自校验能不能开强制"的前置验证：运行期摘要由 Android 的映射形态决定，
     * 必须先证明它等于构建期注入的期望值（也就是静态工具从文件算出的那个），
     * 否则一旦开启强制模式，正常用户会被自己的自校验拦死。
     *
     * 无论通过与否都把完整报告打进 logcat，便于采集各机型的映射形态：
     *   adb logcat -s TestRunner:I | grep SELFCHECK
     */
    @Test
    fun selfCheckDigestMatchesInjectedExpectation() {
        val report = JSONObject(ResolverNative.selfIntegrityCheck())
        android.util.Log.i("TestRunner", "SELFCHECK $report")

        val digest = report.optString("text_digest", "")
        val expected = report.optString("expected_digest", "")
        assertTrue("应算出可执行段摘要（长度 64）: $digest", digest.length == 64)
        assertTrue("产物应带构建期注入的期望摘要: $expected", expected.length == 64)
        assertTrue(
            "运行期摘要必须等于注入的期望摘要（否则强制模式会误拦正常用户）\n" +
                "  runtime = $digest\n  expected= $expected",
            report.optBoolean("text_digest_match", false),
        )
        // 灰度观察期：只上报不拦截
        assertFalse("当前应为灰度（不强制）: $report", report.optBoolean("digest_enforced", true))
        assertFalse("灰度期不应因摘要而拒绝: $report", report.optBoolean("deny", false))
        val strong = report.optJSONArray("injection_strong")
        assertEquals("干净设备上不应有强注入特征: $report", 0, strong?.length() ?: 0)
    }

    @Test
    fun parseTaskThroughContractReturnsQuestionsAndMeta() {
        val content = """
            {"structure_type":"collector.role","info":{"stid":"1","question":[
              {"ask":"Where is the club going to meet?","std":[{"value":"In the school library."}]}]}}
        """.trimIndent()
        val resp = ResolverContract.parseTask(content)
        assertTrue("解析应成功: ${resp.errorCode} ${resp.errorMessage}", resp.ok)
        val questions = resp.resultArray
        assertNotNull("应返回题目数组", questions)
        assertEquals("应解析出 1 题", 1, questions!!.length())
        assertEquals("In the school library.", questions.getJSONObject(0).getJSONArray("answers").getString(0))
        // meta：这是切到契约后才有的诊断信息
        assertEquals("explicit", resp.layer)
        assertEquals("collector.role", resp.family)
        assertTrue("真实题型应被 spec 收录", resp.specKnown)
        assertNotNull("应带结构指纹", resp.shapeHash)
    }

    @Test
    fun diagnoseReportsShapeForUnknownStructure() {
        val content = """{"structure_type":"collector.brand_new_2030","info":{"stid":"1","key":[{"weird":1}]}}"""
        val report = ResolverContract.diagnose(content)
        assertTrue(report.ok)
        val shape = report.result?.optJSONObject("shape")?.optString("hash").orEmpty()
        assertTrue("未知结构应给出形状指纹", shape.isNotBlank())
        assertFalse("未知结构 spec_known 应为 false", report.specKnown)
    }

    @Test
    fun classifyHomeworkDetectsExerciseSet() {
        val hw = JSONObject(
            """{"id":"5","name":"Unit 3 综合练习","set_id":"s-1",
                "contents":[{"group_name":"模仿朗读"},{"group_name":"信息转述"}]}""".trimIndent(),
        )
        val resp = ResolverContract.classifyHomework(hw)
        assertTrue(resp.ok)
        val result = resp.result!!
        assertTrue("多包作业应判为习题集", result.optBoolean("is_exercise_set", false))
        assertEquals("paper_set", result.optString("category"))
        assertTrue("应给出卷面分口径", result.optDouble("expected_total_score", 0.0) > 0.0)
    }

    @Test
    fun parseWordPkUsesOfficialQuestionTypeName() {
        val payload = JSONObject(
            """{"words":[{"id":1,"content":"apple","translate":"苹果"}],
                "word_questions":[{"question_type":6,"word_id":1,"question_type_name":"听音选义",
                  "answer":"A","option":[{"option_name":"A","option_value":"苹果"}]}]}""".trimIndent(),
        )
        val resp = ResolverContract.call("parse_word_pk", JSONObject().put("data", payload))
        assertTrue("词汇解析应成功: ${resp.errorCode}", resp.ok)
        val q = resp.resultArray!!.getJSONObject(0)
        assertEquals("word.choice", q.optString("type"))
        assertEquals("sync_pk", q.optString("channel"))
        assertEquals("听音选义", q.optString("question_type_name"))
        assertEquals("选项字母应与官方答案一致", "A", q.optJSONArray("answers")!!.getString(0))
    }

    @Test
    fun homeworkSetMergeUsesOfficialTemplateWhenAvailable() {
        val resJson = JSONObject(
            """{"set_score":"20","exam_type_list":[
                 {"exam_type_order":"1","exam_type_name":"听选信息","exam_type_collect":"listen_choice_standard",
                  "exam_type_score":"6","exam_list":[{"exam_id":"{{st1}}","exam_score":"6",
                  "question_list":[{"question_order":"1","question_score":"3"},{"question_order":"2","question_score":"3"}]}]}]}""".trimIndent(),
        )
        val packages = JSONArray().put(
            JSONObject(
                """{"structure_type":"collector.role","group_name":"听选信息","code_id":"st1",
                    "questions":[{"question":"Q1","answers":["A"]},{"question":"Q2","answers":["B"]}]}""".trimIndent(),
            ),
        ).put(
            JSONObject(
                """{"structure_type":"collector.picture","group_name":"信息转述","code_id":"st2",
                    "questions":[{"question":"Q3","answers":["C"]}]}""".trimIndent(),
            ),
        )
        // 两个包才会触发整卷合并（单包不做）
        val resp = ResolverContract.parseHomeworkSet(packages, resJson.toString())
        assertTrue("合并应成功: ${resp.errorCode} ${resp.errorMessage}", resp.ok)
        val paper = resp.result!!.optJSONObject("paper")!!
        assertEquals("卷面总分应取官方模板", 20.0, paper.optDouble("totalScore"), 0.001)
        // 给了 2 个包 -> 合并出 2 个大题：第 1 个挂上官方模板（听选信息，2 题），
        // 第 2 个（信息转述）没有对应模板大题，按原样保留（不丢题）。
        val sections = paper.optJSONArray("sections")!!
        assertEquals(2, sections.length())
        val questions = sections.getJSONObject(0).optJSONArray("questions")!!
        assertEquals(2, questions.length())
        assertEquals("3.0", questions.getJSONObject(0).optString("score"))
        assertEquals("1", questions.getJSONObject(0).optString("no"))
        assertEquals("2", questions.getJSONObject(1).optString("no"))

        // 完整性对账：设备上必须能拿到（缺题提示依赖它）
        //
        // 口径（实测，宿主与真机一致）：
        //   sections_weak_mount  = **有包、但没挂上官方模板大题** <- 本用例属这种
        //   sections_not_covered = 官方模板里有大题、但没有任何包挂上去（反方向）
        // 弱挂载的大题会被原样保留（不丢题），并出现在 sections_weak_mount 里。
        val completeness = resp.result!!.optJSONObject("completeness")
        assertNotNull("新版解析器应返回 completeness", completeness)
        val weakMount = completeness!!.optJSONArray("sections_weak_mount")
        assertNotNull("应带 sections_weak_mount", weakMount)
        assertTrue(
            "第 2 个包没挂上官方模板大题，应出现在 sections_weak_mount: $completeness",
            weakMount!!.length() >= 1,
        )
        assertEquals(2, completeness.optInt("template_questions"))
        assertEquals(3, completeness.optInt("parsed_questions"))
        // 弱挂载的大题在 paper 里保留（不因为没模板就丢题）
        assertEquals(
            "未在模板中登记的大题",
            sections.getJSONObject(1).optString("caption"),
        )
    }

    @Test
    fun authorizedEntryPointReturnsStructuredEnvelope() {
        // 未配置 LA Studio 授权时应返回结构化错误信封（证明 JNI 调用链通、不崩）
        val raw = ResolverNative.parseTaskAuthorized("""{"structure_type":"collector.fill"}""", "", "")
        val json = JSONObject(raw)
        assertTrue("应返回 JSON 信封", json.has("ok"))
        if (!json.optBoolean("ok")) {
            assertTrue("失败时应带 error.code", json.optJSONObject("error")?.has("code") == true)
        }
    }

    @Test
    fun contractVersionIsSupported() {
        val resp = ResolverContract.call("capabilities")
        assertEquals(1, resp.raw.optInt("version", -1))
        assertEquals("capabilities", resp.op)
        // 客户端启动时的能力探测（业务可据此决定是否使用某 op）
        assertTrue("应支持 parse_homework_set", ResolverContract.supports("parse_homework_set"))
        assertTrue("应支持 classify_homework", ResolverContract.supports("classify_homework"))
    }
}
