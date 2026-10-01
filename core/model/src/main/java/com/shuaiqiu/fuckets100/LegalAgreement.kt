package com.shuaiqiu.fuckets100

data class LegalAgreement(
    val title: String,
    val effectiveDate: String,
    val version: String,
    val scope: String,
    val sections: List<LegalAgreementSection>,
)

data class LegalAgreementSection(
    val id: String,
    val title: String,
    val badge: String? = null,
    val warning: Boolean = false,
    val content: String? = null,
    val items: List<LegalAgreementItem> = emptyList(),
)

data class LegalAgreementItem(
    val title: String,
    val content: String,
)
