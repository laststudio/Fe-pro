package com.shuaiqiu.fuckets100.feature.account

import androidx.compose.runtime.Composable

@Composable
fun AccountSessionsPage(onBack: () -> Unit) {
    AccountRecordsPage(kind = AccountRecordKind.Session, onBack = onBack)
}
