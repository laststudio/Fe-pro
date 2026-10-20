package com.shuaiqiu.fuckets100

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AppIntegrityGuardTest {
    private val official = "a".repeat(64)

    @Test
    fun `missing expected certificate fails closed`() {
        assertNotNull(signingIntegrityFailure("", official))
        assertNotNull(signingIntegrityFailure(" ", official))
    }

    @Test
    fun `invalid expected certificate fails closed`() {
        assertNotNull(signingIntegrityFailure("a".repeat(63), official))
        assertNotNull(signingIntegrityFailure("g".repeat(64), official))
    }

    @Test
    fun `unreadable actual certificate is rejected`() {
        assertEquals("签名证书不匹配", signingIntegrityFailure(official, null))
    }

    @Test
    fun `debug or resigned certificate is rejected`() {
        assertEquals("签名证书不匹配", signingIntegrityFailure(official, "b".repeat(64)))
    }

    @Test
    fun `official certificate is accepted case insensitively`() {
        assertNull(signingIntegrityFailure(official, official.uppercase()))
    }
}
