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

    /**
     * 契约的"出答案"op 在 Android 上必须有授权门禁。
     *
     * 背景：此前 `resolve_request` 的 parse 类 op 完全没有授权检查（libcloud 与旧接口都有），
     * 等于把"库内部强制联网校验、补丁掉 Java 层也没用"这条设计目标架空。现在对
     * parse_task / parse_batch / parse_word_pk / parse_homework_set 加了门禁（仅 Android 目标）。
     *
     * 本用例在**未配置授权**的测试进程里跑，因此必须拿到授权类错误码，而不是答案：
     * 这正好证明门禁在真机产物里是活的。
     * （带授权时的解析正确性由 148 个 Rust 用例 + 宿主 JNI 探针覆盖。）
     */
    @Test
    fun parseOpsRequireAuthorizationOnAndroid() {
        val content = """
            {"structure_type":"collector.role","info":{"stid":"1","question":[
              {"ask":"Where is the club going to meet?","std":[{"value":"In the school library."}]}]}}
        """.trimIndent()

        val parseResp = ResolverContract.parseTask(content)
        assertFalse("未授权时 parse_task 必须被拒: ${parseResp.raw}", parseResp.ok)
        val code = parseResp.errorCode.orEmpty()
        assertTrue(
            "应是授权类错误码（实际 $code）：${parseResp.raw}",
            code.startsWith("auth_") || code == "network_error",
        )
        assertFalse(
            "被拒时不应泄露答案: ${parseResp.raw}",
            parseResp.raw.toString().contains("school library"),
        )

        val gated = listOf(
            "parse_task" to JSONObject().put("content", JSONObject(content)),
            "parse_batch" to JSONObject().put("tasks", JSONArray()),
            "parse_word_pk" to JSONObject().put("data", JSONObject().put("words", JSONArray())),
            "parse_homework_set" to JSONObject().put("packages", JSONArray()),
        )
        for ((op, payload) in gated) {
            val r = ResolverContract.call(op, payload)
            assertFalse("$op 必须走授权门禁: ${r.raw}", r.ok)
            val c = r.errorCode.orEmpty()
            assertTrue("$op 应是授权类错误码（实际 $c）", c.startsWith("auth_") || c == "network_error")
        }
    }

    /** 遥测/元数据类 op 保持免费：授权被拒时也要能上报结构指纹（否则发现不了新题型）。 */
    @Test
    fun diagnosticOpsStayFreeWithoutAuthorization() {
        val content = """{"structure_type":"collector.brand_new_2030","info":{"stid":"1","key":[{"weird":1}]}}"""
        val report = ResolverContract.diagnose(content)
        assertTrue("diagnose 不应被门禁拦: ${report.raw}", report.ok)

        val caps = ResolverContract.capabilities()
        assertTrue("capabilities 不应被门禁拦: ${caps.raw}", caps.ok)

        val cls = ResolverContract.classifyHomeworkList(JSONArray())
        assertTrue("classify_homework_list 不应被门禁拦: ${cls.raw}", cls.ok)
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

    /** 整卷合并同样属于"出答案"的 op：未授权时必须被拦。 */
    @Test
    fun homeworkSetMergeRequiresAuthorization() {
        val packages = JSONArray().put(
            JSONObject("""{"structure_type":"collector.role","group_name":"听选信息","code_id":"st1",
                "questions":[{"question":"Q1","answers":["A"]}]}""".trimIndent()),
        ).put(
            JSONObject("""{"structure_type":"collector.picture","group_name":"信息转述","code_id":"st2",
                "questions":[{"question":"Q3","answers":["C"]}]}""".trimIndent()),
        )
        val resp = ResolverContract.parseHomeworkSet(packages, null)
        assertFalse("未授权时合并必须被拒: ${resp.raw}", resp.ok)
        val code = resp.errorCode.orEmpty()
        assertTrue("应是授权类错误码（实际 $code）", code.startsWith("auth_") || code == "network_error")
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
