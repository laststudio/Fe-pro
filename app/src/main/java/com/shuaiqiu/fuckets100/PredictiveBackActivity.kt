package com.shuaiqiu.fuckets100

import androidx.activity.ComponentActivity

internal fun ComponentActivity.applyPredictiveBackWindowTheme() {
    setTheme(R.style.Theme_Fe)
}

internal fun <T : ComponentActivity> predictiveBackActivityClass(
    opaqueActivity: Class<out T>,
): Class<out T> = opaqueActivity
