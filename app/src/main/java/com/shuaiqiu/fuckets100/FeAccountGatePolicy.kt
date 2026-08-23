package com.shuaiqiu.fuckets100

internal sealed interface FeSoftwarePolicyDecision {
    data object Allowed : FeSoftwarePolicyDecision
    data class Blocked(val message: String) : FeSoftwarePolicyDecision
    data class ForceUpdate(val message: String, val updateUrl: String?) : FeSoftwarePolicyDecision
}

internal fun isRetryableFeGateStatus(statusCode: Int): Boolean =
    statusCode == 403 || statusCode == 429 || statusCode >= 500

internal fun evaluateFeSoftwarePolicy(
    currentVersionCode: Int,
    killSwitch: Boolean,
    latestVersionCode: Int,
    forceUpdate: Boolean,
    message: String,
    updateUrl: String,
): FeSoftwarePolicyDecision = when {
    killSwitch -> FeSoftwarePolicyDecision.Blocked(message)
    forceUpdate && latestVersionCode > currentVersionCode -> FeSoftwarePolicyDecision.ForceUpdate(
        message = message,
        updateUrl = updateUrl.takeIf { it.isNotBlank() },
    )
    else -> FeSoftwarePolicyDecision.Allowed
}
