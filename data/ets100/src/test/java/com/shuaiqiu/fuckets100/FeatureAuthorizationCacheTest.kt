package com.shuaiqiu.fuckets100

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAuthorizationCacheTest {
    @Test
    fun `same token and unchanged policy can reuse unexpired authorization`() {
        assertTrue(FeatureAuthorizationCache("token", 100, true).isValid("token", 101, true, 1000))
        assertTrue(FeatureAuthorizationCache("token", 100, false).isValid("token", 101, false, 1000))
    }

    @Test
    fun `turning integrity on invalidates authorization obtained while off`() {
        assertFalse(FeatureAuthorizationCache("token", 100, false).isValid("token", 101, true, 1000))
    }

    @Test
    fun `turning integrity off also forces native configuration refresh`() {
        assertFalse(FeatureAuthorizationCache("token", 100, true).isValid("token", 101, false, 1000))
    }

    @Test
    fun `changed token or expired authorization is not reused`() {
        val cache = FeatureAuthorizationCache("token", 100, false)
        assertFalse(cache.isValid("new-token", 101, false, 1000))
        assertFalse(cache.isValid("token", 1100, false, 1000))
        assertFalse(cache.isValid("token", 99, false, 1000))
    }
}
