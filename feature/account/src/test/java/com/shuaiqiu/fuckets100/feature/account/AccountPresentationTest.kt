package com.shuaiqiu.fuckets100.feature.account

import org.junit.Assert.*
import org.junit.Test

class AccountPresentationTest {
    @Test fun emptyPasswordFieldsAreRejected() {
        assertEquals("请填写完整", validateAccountPasswordChange("", "newpassword", "newpassword"))
        assertEquals("请填写完整", validateAccountPasswordChange("old", "", ""))
        assertEquals("请填写完整", validateAccountPasswordChange("old", "newpassword", ""))
    }
    @Test fun mismatchingPasswordsAreRejected() {
        assertEquals("两次新密码不一致", validateAccountPasswordChange("old", "newpassword", "differentpw"))
    }
    @Test fun existingMinimumLengthIsPreserved() {
        assertEquals("新密码至少 10 位", validateAccountPasswordChange("old", "123456789", "123456789"))
        assertNull(validateAccountPasswordChange("old", "1234567890", "1234567890"))
    }
    @Test fun passwordContentsAreNotTrimmed() {
        assertNull(validateAccountPasswordChange("old", " password ", " password "))
        assertEquals("两次新密码不一致", validateAccountPasswordChange("old", " password ", "password"))
    }
    @Test fun missingIdsNeverIdentifyCurrentRecord() {
        assertFalse(isCurrentAccountRecord("", null))
        assertFalse(isCurrentAccountRecord("", ""))
        assertFalse(isCurrentAccountRecord("device", ""))
    }
    @Test fun onlyExactIdMatchIdentifiesCurrentRecord() {
        assertTrue(isCurrentAccountRecord("device-a", "device-a"))
        assertFalse(isCurrentAccountRecord("device-a", "device-b"))
    }
    @Test fun statusDoesNotImplyAnOnlinePresence() {
        assertEquals("有效", accountRecordStatus("active"))
        assertEquals("已下线", accountRecordStatus("revoked"))
        assertEquals("已过期", accountRecordStatus("expired"))
        assertEquals("未知状态", accountRecordStatus(""))
    }
    @Test fun platformNamesAreReadableAndUnknownValuesArePreserved() {
        assertEquals("Android", accountPlatformName("android"))
        assertEquals("iOS", accountPlatformName("IOS"))
        assertEquals("macOS", accountPlatformName("darwin"))
        assertEquals("custom", accountPlatformName("custom"))
        assertEquals("未知平台", accountPlatformName(""))
    }
    @Test fun knownClientsHaveCompactNames() {
        assertEquals("Fe · Android", accountClientName("Fe/2.1.0 Android"))
        assertEquals("Fe 客户端", accountClientName("Fe/2.1.0"))
        assertEquals("网页客户端", accountClientName("Mozilla/5.0"))
        assertEquals("未知客户端", accountClientName(""))
        assertEquals("其他客户端", accountClientName("custom/1.0"))
    }
}
