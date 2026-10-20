package com.shuaiqiu.fuckets100.feature.read

/** 点击读取和下拉读取共用的顶号警告策略。 */
internal fun requestCloudReadWithWarning(
    skipWarning: Boolean,
    onWarningRequired: () -> Unit,
    onRead: () -> Unit,
) {
    if (skipWarning) {
        onRead()
    } else {
        onWarningRequired()
    }
}
