package com.shuaiqiu.fuckets100

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 锁定从 ETS100ApiClient 拆出的纯函数行为（JSON 序列化 / protobuf 解析 / 账号字段解析）。
 */
class ETS100ApiHelpersTest {
    @Test
    fun parseJsonAcceptsObjectPayload() {
        val parsed = """{"code":0,"message":"ok"}""".parseJson()

        assertEquals(0, parsed.optInt("code"))
        assertEquals("ok", parsed.optString("message"))
    }

    @Test
    fun parseJsonUnwrapsFirstElementOfArrayPayload() {
        val parsed = """[{"code":0},{"code":1}]""".parseJson()

        assertEquals(0, parsed.optInt("code"))
    }

    @Test
    fun parseJsonReturnsEmptyObjectForEmptyArray() {
        assertTrue("{}".isNotEmpty())
        assertEquals(0, "[]".parseJson().length())
    }

    @Test
    fun isJsonArrayOnlyMatchesArrayPayloads() {
        assertTrue("  [1,2,3]".isJsonArray())
        assertFalse("""{"a":1}""".isJsonArray())
    }

    @Test
    fun toJsonSerializesNestedCollectionsAndEscapesStrings() {
        val payload: Map<String, Any?> = mapOf(
            "login" to "a\"b",
            "count" to 2,
            "flag" to true,
            "nested" to mapOf("k" to "v"),
            "list" to listOf(1, "x"),
            "none" to null,
        )

        val json = JSONObject(payload.toJsonObject())

        assertEquals("a\"b", json.getString("login"))
        assertEquals(2, json.getInt("count"))
        assertTrue(json.getBoolean("flag"))
        assertEquals("v", json.getJSONObject("nested").getString("k"))
        assertEquals(2, json.getJSONArray("list").length())
        assertTrue(json.isNull("none"))
    }

    @Test
    fun escapeJsonEscapesControlCharacters() {
        assertEquals("a\\\\b", "a\\b".escapeJson())
        assertEquals("a\\\"b", "a\"b".escapeJson())
        assertEquals("a\\nb", "a\nb".escapeJson())
        assertEquals("a\\tb", "a\tb".escapeJson())
    }

    @Test
    fun toMapJsonWrapsFragmentsInBraces() {
        assertEquals("""{"a":1}""", """"a":1""".toMapJson())
    }

    @Test
    fun maskSensitiveTokenHidesMiddleOfLongTokens() {
        assertEquals("(empty)", maskSensitiveToken("   "))
        assertEquals("abc***xyz", maskSensitiveToken("abcdefxyz"))
        assertEquals("abcdef***uvwxyz", maskSensitiveToken("abcdefghijklmnopuvwxyz"))
    }

    @Test
    fun parseEcardAccountsReadsAllAccountFieldsInKeyOrder() {
        val body = JSONObject()
            .put(
                "ecard-1",
                JSONObject()
                    .put("id", "id-1")
                    .put("parent_id", "parent-1")
                    .put("user_account_id", "user-1")
                    .put("name", "小明")
                    .put("grade", "七年级")
                    .put("status", "0")
                    .put("mobile_status", "1")
                    .put("out_of_date", "0")
                    .put("class_id", "class-1")
                    .put("class_name", "一班")
                    .put("machine_code_status", "ok"),
            )

        val accounts = parseEcardAccounts(body)

        assertEquals(1, accounts.size)
        assertEquals("ecard-1", accounts[0].key)
        assertEquals("id-1", accounts[0].id)
        assertEquals("parent-1", accounts[0].parentId)
        assertEquals("user-1", accounts[0].userAccountId)
        assertEquals("小明", accounts[0].name)
        assertEquals("七年级", accounts[0].grade)
        assertEquals("class-1", accounts[0].classId)
        assertEquals("一班", accounts[0].className)
        assertEquals("ok", accounts[0].machineCodeStatus)
        assertTrue(accounts[0].isValid)
    }

    @Test
    fun ecardAccountIsInvalidWhenStatusOrClassOrMobileIsMissing() {
        val base = JSONObject()
            .put("id", "id")
            .put("parent_id", "parent")
            .put("status", "0")
            .put("mobile_status", "1")
            .put("out_of_date", "0")
            .put("class_id", "class")

        val validBody = JSONObject().put("a", JSONObject(base.toString()))
        assertTrue(parseEcardAccounts(validBody)[0].isValid)

        val inactive = JSONObject().put("a", JSONObject(base.toString()).put("status", "1"))
        assertFalse(parseEcardAccounts(inactive)[0].isValid)

        val expired = JSONObject().put("a", JSONObject(base.toString()).put("out_of_date", "1"))
        assertFalse(parseEcardAccounts(expired)[0].isValid)

        val noClass = JSONObject().put("a", JSONObject(base.toString()).put("class_id", ""))
        assertFalse(parseEcardAccounts(noClass)[0].isValid)

        val mobileOff = JSONObject().put("a", JSONObject(base.toString()).put("mobile_status", "0"))
        assertFalse(parseEcardAccounts(mobileOff)[0].isValid)
    }

    @Test
    fun parseEcardAccountsIgnoresNonObjectEntries() {
        val body = JSONObject()
            .put("ok", JSONObject().put("id", "1"))
            .put("scalar", "not-an-object")
            .put("array", JSONArray().put(1))

        val accounts = parseEcardAccounts(body)

        assertEquals(1, accounts.size)
        assertEquals("ok", accounts[0].key)
    }
}
