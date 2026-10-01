package com.shuaiqiu.fuckets100.feature.read

import android.util.Log

/**
 * 云端读取诊断日志。
 *
 * 与界面层解耦：loader 与 ViewModel 都能写日志，但不需要把 Compose 的日志
 * 状态或回调向上传递。
 */
internal object ReadCloudLog {
    private const val TAG = "ReadScreen"

    fun info(message: String) = Log.i(TAG, "[云端] $message")
    fun debug(message: String) = Log.i(TAG, "[云端] $message")
    fun success(message: String) = Log.i(TAG, "[云端] $message")
    fun warn(message: String) = Log.w(TAG, "[云端] $message")
    fun error(message: String) = Log.e(TAG, "[云端] $message")
}
