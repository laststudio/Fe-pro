package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 设置管理器
 * 负责保存和读取应用的各种设置选项喵~
 */
object SettingsManager {
    
    private const val PREFS_NAME = "fe_settings"
    private const val KEY_ACTIVATION_MODE = "activation_mode"
    private const val KEY_LEGAL_ACCEPTED = "legal_accepted"
    private const val KEY_AOSP_PREDICTIVE_BACK = "aosp_predictive_back"
    private const val KEY_PREDICTIVE_BACK_MODE = "predictive_back_mode"
    private const val KEY_LOCAL_VERIFICATION_CODE = "local_verification_code"
    private const val KEY_COMPACT_ANSWER_DISPLAY = "compact_answer_display"
    
    private lateinit var prefs: SharedPreferences
    
    /**
     * 初始化设置管理器，在 Application 启动时调用
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 保存激活模式设置
     */
    fun saveActivationMode(mode: ActivationMode) {
        prefs.edit {
            putString(KEY_ACTIVATION_MODE, mode.name)
        }
    }
    
    /**
     * 读取保存的激活模式设置
     * 返回 null 表示用户尚未选择激活模式
     */
    fun getSavedActivationMode(): ActivationMode? {
        val modeName = prefs.getString(KEY_ACTIVATION_MODE, null) ?: return null
        return try {
            ActivationMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            null
        }
    }
    
    /**
     * 检查用户是否已经选择了激活模式
     */
    fun hasUserSelectedMode(): Boolean {
        return prefs.contains(KEY_ACTIVATION_MODE)
    }
    
    /**
     * 清除保存的激活模式设置
     */
    fun clearActivationMode() {
        prefs.edit {
            remove(KEY_ACTIVATION_MODE)
        }
    }
    
    fun saveCompactAnswerDisplay(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_COMPACT_ANSWER_DISPLAY, enabled)
        }
    }

    fun getCompactAnswerDisplay(): Boolean {
        return prefs.getBoolean(KEY_COMPACT_ANSWER_DISPLAY, false)
    }

    fun saveLegalAccepted(accepted: Boolean) {
        prefs.edit {
            putBoolean(KEY_LEGAL_ACCEPTED, accepted)
        }
    }

    fun hasAcceptedLegal(): Boolean {
        return prefs.getBoolean(KEY_LEGAL_ACCEPTED, false)
    }

    fun saveLocalVerificationCode(code: String) {
        prefs.edit {
            putString(KEY_LOCAL_VERIFICATION_CODE, code.trim())
        }
    }

    fun getLocalVerificationCode(): String {
        return prefs.getString(KEY_LOCAL_VERIFICATION_CODE, "").orEmpty()
    }

    fun savePredictiveBackMode(mode: PredictiveBackMode) {
        prefs.edit {
            putString(KEY_PREDICTIVE_BACK_MODE, mode.name)
            putBoolean(KEY_AOSP_PREDICTIVE_BACK, mode == PredictiveBackMode.AOSP)
        }
    }

    fun getPredictiveBackMode(): PredictiveBackMode {
        val modeName = prefs.getString(KEY_PREDICTIVE_BACK_MODE, null)
        if (modeName != null) {
            return runCatching { PredictiveBackMode.valueOf(modeName) }
                .getOrDefault(PredictiveBackMode.AOSP)
        }

        return if (prefs.getBoolean(KEY_AOSP_PREDICTIVE_BACK, true)) {
            PredictiveBackMode.AOSP
        } else {
            PredictiveBackMode.SLIDE
        }
    }

    fun saveAospPredictiveBackEnabled(enabled: Boolean) {
        savePredictiveBackMode(if (enabled) PredictiveBackMode.AOSP else PredictiveBackMode.SLIDE)
    }

    fun isAospPredictiveBackEnabled(): Boolean {
        return getPredictiveBackMode() == PredictiveBackMode.AOSP
    }
}

enum class PredictiveBackMode(
    val label: String,
    val description: String
) {
    AOSP("缩放", "使用缩放式预见性返回动画"),
    KERNELSU_CLASSIC("渐变", "使用渐变式预见性返回动画"),
    SLIDE("滑动", "使用系统自带预见性返回动画"),
    NONE("无", "关闭自定义预见性返回动画")
}
