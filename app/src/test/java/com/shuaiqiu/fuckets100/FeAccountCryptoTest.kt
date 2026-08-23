package com.shuaiqiu.fuckets100

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeAccountCryptoTest {
    @Test
    fun `certificate bytes are SHA-256 digested to lowercase hex`() {
        val digest = sha256LowerHex(byteArrayOf(0x01, 0x02, 0x03, 0x7f))

        assertEquals("26981fe24cb5abc63ad522541a14386572201625c1f292aa11312b9dafa5d947", digest)
        assertEquals(64, digest.length)
        assertTrue(digest.matches(Regex("[0-9a-f]{64}")))
    }
}
