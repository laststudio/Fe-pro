package com.shuaiqiu.fuckets100

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeAccountGatePolicyTest {
    @Test
    fun `kill switch blocks regardless of version`() {
        val decision = evaluateFeSoftwarePolicy(17, true, 17, false, "维护中", "")
        assertEquals(FeSoftwarePolicyDecision.Blocked("维护中"), decision)
    }

    @Test
    fun `newer forced release requires update`() {
        val decision = evaluateFeSoftwarePolicy(17, false, 18, true, "请更新", "https://example.test/app")
        assertEquals(
            FeSoftwarePolicyDecision.ForceUpdate("请更新", "https://example.test/app"),
            decision,
        )
    }

    @Test
    fun `optional release does not block startup`() {
        val decision = evaluateFeSoftwarePolicy(17, false, 18, false, "有新版本", "")
        assertTrue(decision is FeSoftwarePolicyDecision.Allowed)
    }

    @Test
    fun `retry is offered only for meaningful gate statuses`() {
        assertTrue(isRetryableFeGateStatus(403))
        assertTrue(isRetryableFeGateStatus(429))
        assertTrue(isRetryableFeGateStatus(503))
        assertTrue(!isRetryableFeGateStatus(400))
        assertTrue(!isRetryableFeGateStatus(401))
    }

    @Test
    fun `software policy and remote config integrity flag defaults to true`() {
        val policyDefault = FeAccountApi.SoftwarePolicy(
            killSwitch = false,
            latestVersionCode = 17,
            forceUpdate = false,
            updateUrl = "",
            message = "",
        )
        assertTrue(policyDefault.integrityCheckEnabled)

        val policyDisabled = policyDefault.copy(integrityCheckEnabled = false)
        assertTrue(!policyDisabled.integrityCheckEnabled)

        val remoteConfig = RemoteConfig(
            latestVersionCode = 17,
            updateUrl = "",
            isKillSwitchOn = false,
            isForce = false,
            updateMessage = "",
            noticeMessage = "",
        )
        assertTrue(remoteConfig.integrityCheckEnabled)
    }

    @Test
    fun `session integrity check defaults to true and can be disabled`() {
        val sessionDefault = FeAccountSession(
            sessionId = "sess-1",
            deviceId = "dev-1",
            user = FeAccountUser(1, "tester", 1),
        )
        assertTrue(sessionDefault.integrityCheckEnabled)

        val sessionDisabled = sessionDefault.copy(integrityCheckEnabled = false)
        assertTrue(!sessionDisabled.integrityCheckEnabled)
    }

    @Test
    fun `version feature switches in RemoteConfig and SoftwarePolicy`() {
        val policyWithFeatures = FeAccountApi.SoftwarePolicy(
            killSwitch = false,
            latestVersionCode = 17,
            forceUpdate = false,
            updateUrl = "",
            message = "",
            features = mapOf("answer_parser" to true, "cloud_mode" to false),
        )
        assertEquals(true, policyWithFeatures.features["answer_parser"])
        assertEquals(false, policyWithFeatures.features["cloud_mode"])

        val remoteConfig = RemoteConfig(
            latestVersionCode = 17,
            updateUrl = "",
            isKillSwitchOn = false,
            isForce = false,
            updateMessage = "",
            noticeMessage = "",
            features = mapOf("cloud_mode" to false),
        )
        assertEquals(false, remoteConfig.features["cloud_mode"])
        assertEquals(null, remoteConfig.features["unknown_feature"])

        val featureConfig = FeFeatureConfig(
            featureKey = "answer_parser",
            name = "答案解析",
            availableForVersion = false,
            reason = "客户端版本过低，请先更新软件（最低要求 20）",
        )
        assertEquals(false, featureConfig.availableForVersion)
        assertEquals("客户端版本过低，请先更新软件（最低要求 20）", featureConfig.reason)
    }

    @Test
    fun `answer share feature constants and configuration`() {
        assertEquals("answer_share", FeAccountFeatures.ANSWER_SHARE)

        val enabledConfig = FeFeatureConfig(
            featureKey = FeAccountFeatures.ANSWER_SHARE,
            name = "答案分享",
            status = "enabled",
            availableForVersion = true,
            requiresMembership = false,
        )
        assertEquals(true, enabledConfig.availableForVersion)
        assertEquals("enabled", enabledConfig.status)

        val disabledConfig = FeFeatureConfig(
            featureKey = FeAccountFeatures.ANSWER_SHARE,
            name = "答案分享",
            status = "disabled",
            availableForVersion = false,
            reason = "答案分享功能当前不可用",
        )
        assertEquals(false, disabledConfig.availableForVersion)
        assertEquals("答案分享功能当前不可用", disabledConfig.reason)
    }
}

