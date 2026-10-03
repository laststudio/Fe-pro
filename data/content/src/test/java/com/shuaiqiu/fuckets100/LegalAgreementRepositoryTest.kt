package com.shuaiqiu.fuckets100

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalAgreementRepositoryTest {

    @Test
    fun `fallback agreement is well formed with all core sections`() {
        val agreement = LegalAgreementRepository.FALLBACK_AGREEMENT
        assertEquals("LastStudio 服务协议、隐私政策与免责声明", agreement.title)
        assertEquals("2026-09-25", agreement.version)
        assertEquals("2026 年 9 月 25 日", agreement.effectiveDate)
        assertTrue(agreement.scope.contains("Fuck ETS100"))
        assertEquals(6, agreement.sections.size)

        val preface = agreement.sections[0]
        assertEquals("preface", preface.id)
        assertEquals("请先阅读", preface.badge)
        assertTrue(preface.warning)
        assertNotNull(preface.content)

        val sec1 = agreement.sections[1]
        assertEquals("section-1", sec1.id)
        assertEquals("服务规则", sec1.badge)
        assertEquals(5, sec1.items.size)

        val sec4 = agreement.sections[4]
        assertEquals("section-4", sec4.id)
        assertTrue(sec4.warning)
        assertEquals("风险告知", sec4.badge)
    }

    @Test
    fun `parseAgreement parses valid backend json response correctly`() {
        val json = """
        {
            "title": "测试服务协议",
            "effective_date": "2026 年 10 月 1 日",
            "version": "2026-10-01",
            "scope": "测试客户端",
            "sections": [
                {
                    "id": "intro",
                    "title": "测试前言",
                    "badge": "重点",
                    "warning": true,
                    "content": "前言内容测试",
                    "items": []
                },
                {
                    "id": "rules",
                    "title": "测试规则",
                    "badge": "规则",
                    "warning": false,
                    "content": null,
                    "items": [
                        {
                            "title": "1. 规则一条",
                            "content": "内容详细描述"
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val parsed = LegalAgreementRepository.parseAgreement(json)
        assertEquals("测试服务协议", parsed.title)
        assertEquals("2026 年 10 月 1 日", parsed.effectiveDate)
        assertEquals("2026-10-01", parsed.version)
        assertEquals("测试客户端", parsed.scope)
        assertEquals(2, parsed.sections.size)

        val s1 = parsed.sections[0]
        assertEquals("intro", s1.id)
        assertEquals("重点", s1.badge)
        assertTrue(s1.warning)
        assertEquals("前言内容测试", s1.content)

        val s2 = parsed.sections[1]
        assertEquals("rules", s2.id)
        assertEquals("规则", s2.badge)
        assertFalse(s2.warning)
        assertEquals(1, s2.items.size)
        assertEquals("1. 规则一条", s2.items[0].title)
        assertEquals("内容详细描述", s2.items[0].content)
    }

    @Test
    fun `parseAgreement falls back gracefully on missing fields`() {
        val emptyJson = "{}"
        val parsed = LegalAgreementRepository.parseAgreement(emptyJson)
        assertEquals(LegalAgreementRepository.FALLBACK_AGREEMENT.title, parsed.title)
        assertEquals(LegalAgreementRepository.FALLBACK_AGREEMENT.version, parsed.version)
        assertEquals(LegalAgreementRepository.FALLBACK_AGREEMENT.sections.size, parsed.sections.size)
    }
}
