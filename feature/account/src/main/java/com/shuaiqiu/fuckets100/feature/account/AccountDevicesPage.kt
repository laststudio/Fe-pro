package com.shuaiqiu.fuckets100.feature.account

import androidx.compose.runtime.Composable

@Composable
fun AccountDevicesPage(onBack: () -> Unit) {
    AccountRecordsPage(kind = AccountRecordKind.Device, onBack = onBack)
}
