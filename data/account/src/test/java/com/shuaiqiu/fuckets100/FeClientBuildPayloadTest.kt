package com.shuaiqiu.fuckets100

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeClientBuildPayloadTest {
    @Test
    fun `current package certificate and version are included`() {
        val payload = clientBuildPayload("com.example.fe", "a".repeat(64), 22, false)

        assertEquals("com.example.fe", payload.getString("application_id"))
        assertEquals("a".repeat(64), payload.getString("signing_certificate_sha256"))
        assertEquals(22, payload.getInt("version_code"))
        assertTrue(payload.has("is_debuggable"))
        assertFalse(payload.getBoolean("is_debuggable"))
    }

    @Test
    fun `debug state is explicit even when the previous session was official`() {
        val official = clientBuildPayload("com.example.fe", "a".repeat(64), 22, false)
        val current = clientBuildPayload("com.example.fe", "b".repeat(64), 22, true)

        assertFalse(official.getBoolean("is_debuggable"))
        assertTrue(current.getBoolean("is_debuggable"))
        assertEquals("b".repeat(64), current.getString("signing_certificate_sha256"))
    }
}
