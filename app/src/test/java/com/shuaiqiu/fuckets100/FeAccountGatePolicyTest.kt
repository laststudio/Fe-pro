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
}
