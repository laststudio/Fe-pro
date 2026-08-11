package com.shuaiqiu.fuckets100

import androidx.activity.ComponentActivity

internal fun ComponentActivity.applyPredictiveBackWindowTheme() {
    setTheme(
        when (SettingsManager.getPredictiveBackAnimation()) {
            PredictiveBackAnimation.AOSP -> R.style.Theme_Fe_Transparent
            PredictiveBackAnimation.KernelSUClassic -> R.style.Theme_Fe_KernelSuClassic
            else -> R.style.Theme_Fe
        }
    )
}

internal fun <T : ComponentActivity> predictiveBackActivityClass(
    transparentActivity: Class<T>,
    opaqueActivity: Class<out T>,
    kernelSuClassicActivity: Class<out T>
): Class<out T> {
    return when (SettingsManager.getPredictiveBackAnimation()) {
        PredictiveBackAnimation.AOSP -> transparentActivity
        PredictiveBackAnimation.KernelSUClassic -> kernelSuClassicActivity
        else -> opaqueActivity
    }
}
