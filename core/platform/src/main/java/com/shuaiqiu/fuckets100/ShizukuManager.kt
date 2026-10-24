package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.BufferedReader
import java.io.FilterInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Method

/**
 * Shizuku 状态监控与管理核心
 * 用于检测和管理 Shizuku 运行状态及权限申请
 */
object ShizukuManager {
    
    private const val TAG = "ShizukuManager"
    
    // Shizuku 下载地址
    const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
    
    // Shizuku 权限请求码
    const val REQUEST_CODE_SHIZUKU_PERMISSION = 1001
    
    // Shizuku 状态监听器列表。保存的是真正注册进 Shizuku 的实例，
    // 这样移除时才能按实例匹配完成注销（Shizuku 内部按 ListenerHolder 相等性查找）。
    private val binderReceivedListeners = mutableListOf<Shizuku.OnBinderReceivedListener>()
    private val binderDeadListeners = mutableListOf<Shizuku.OnBinderDeadListener>()
    private val permissionResultListeners = mutableListOf<Shizuku.OnRequestPermissionResultListener>()
    
    /**
     * 检查 Shizuku 运行时是否正常
     */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查是否为 Sui (Magisk 模块)
     */
    fun isSui(): Boolean = ShizukuRuntimeState.isSui
    
    /**
     * 获取运行时类型描述
     */
    fun getRuntimeType(): String {
        return when {
            isSui() -> "Sui (Magisk)"
            isShizukuRunning() -> when (getUid()) {
                0 -> "Shizuku (Root)"
                2000 -> "Shizuku (ADB)"
                else -> "Shizuku (UID: ${getUid()})"
            }
            else -> "Not Available"
        }
    }
    
    /**
     * 获取 Shizuku/Sui 的 UID
     * 0 = Root, 2000 = ADB Shell
     */
    fun getUid(): Int = ShizukuRuntimeState.uid
    
    /**
     * 获取 Shizuku 版本号
     */
    fun getVersion(): Int = ShizukuRuntimeState.version
    
    /**
     * 检查 Shizuku 版本是否低于 11
     */
    fun isPreV11(): Boolean {
        return try {
            Shizuku.isPreV11()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 检查自身权限
     */
    fun checkSelfPermission(): Int {
        return try {
            Shizuku.checkSelfPermission()
        } catch (e: Exception) {
            Log.e(TAG, "检查自身权限失败", e)
            PackageManager.PERMISSION_DENIED
        }
    }
    
    /**
     * 检查是否需要显示权限申请说明
     */
    fun shouldShowRequestPermissionRationale(): Boolean {
        return try {
            Shizuku.shouldShowRequestPermissionRationale()
        } catch (e: Exception) {
            Log.e(TAG, "检查权限申请说明失败", e)
            false
        }
    }
    
    /**
     * 请求 Shizuku 权限
     */
    fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Exception) {
            Log.e(TAG, "请求权限失败", e)
        }
    }
    
    /**
     * 添加 Binder 连接监听器
     * @return 注册实例，注销时必须把同一实例传给 [removeBinderReceivedListener]
     */
    fun addBinderReceivedListener(listener: () -> Unit): Shizuku.OnBinderReceivedListener {
        val shizukuListener = Shizuku.OnBinderReceivedListener(listener)
        binderReceivedListeners.add(shizukuListener)
        Shizuku.addBinderReceivedListenerSticky(shizukuListener)
        return shizukuListener
    }

    /**
     * 注销 Binder 连接监听器
     */
    fun removeBinderReceivedListener(listener: Shizuku.OnBinderReceivedListener) {
        binderReceivedListeners.remove(listener)
        Shizuku.removeBinderReceivedListener(listener)
    }

    /**
     * 添加 Binder 断开监听器
     * @return 注册实例，注销时必须把同一实例传给 [removeBinderDeadListener]
     */
    fun addBinderDeadListener(listener: () -> Unit): Shizuku.OnBinderDeadListener {
        val shizukuListener = Shizuku.OnBinderDeadListener(listener)
        binderDeadListeners.add(shizukuListener)
        Shizuku.addBinderDeadListener(shizukuListener)
        return shizukuListener
    }

    /**
     * 注销 Binder 断开监听器
     */
    fun removeBinderDeadListener(listener: Shizuku.OnBinderDeadListener) {
        binderDeadListeners.remove(listener)
        Shizuku.removeBinderDeadListener(listener)
    }

    /**
     * 添加权限申请结果监听器
     * @return 注册实例，注销时必须把同一实例传给 [removePermissionResultListener]
     */
    fun addPermissionResultListener(listener: (Int, Int) -> Unit): Shizuku.OnRequestPermissionResultListener {
        val shizukuListener = Shizuku.OnRequestPermissionResultListener(listener)
        permissionResultListeners.add(shizukuListener)
        Shizuku.addRequestPermissionResultListener(shizukuListener)
        return shizukuListener
    }

    /**
     * 注销权限申请结果监听器
     */
    fun removePermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        permissionResultListeners.remove(listener)
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    /**
     * 移除所有监听器
     */
    fun removeAllListeners() {
        binderReceivedListeners.forEach { Shizuku.removeBinderReceivedListener(it) }
        binderReceivedListeners.clear()

        binderDeadListeners.forEach { Shizuku.removeBinderDeadListener(it) }
        binderDeadListeners.clear()

        permissionResultListeners.forEach { Shizuku.removeRequestPermissionResultListener(it) }
        permissionResultListeners.clear()
    }
    
    /**
     * 打开 Shizuku 下载页面
     */
    fun openShizukuDownloadPage(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHIZUKU_DOWNLOAD_URL))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
    
    /**
     * 打开 Shizuku 应用（如果已安装），否则打开下载页面
     */
    fun openShizukuApp(context: Context) {
        try {
            val packageName = "moe.shizuku.privileged.api"
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
            } else {
                // 未找到应用，打开下载页面
                openShizukuDownloadPage(context)
            }
        } catch (e: Exception) {
            openShizukuDownloadPage(context)
        }
    }
    
    /**
     * 使用 Shizuku API 执行命令
     * 宝贝这是修复后的方法，使用反射调用 Shizuku 的 private newProcess API 执行命令喵！
     * @param command 要执行的命令
     * @return 命令输出结果，失败时返回 null
     */
    fun execCommand(command: String): String? {
        return try {
            // 检查 Shizuku 是否可用
            if (!isShizukuRunning()) {
                Log.w(TAG, "execCommand: Shizuku is not running")
                return null
            }
            
            // 检查权限
            if (checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "execCommand: Shizuku permission not granted")
                return null
            }
            
            // 宝贝使用反射调用 Shizuku 的 private newProcess 方法喵~
            // newProcess 签名: public static ShizukuRemoteProcess newProcess(String[] cmd, String[] env, String dir)
            val newProcessMethod: Method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            
            @Suppress("UNCHECKED_CAST")
            val process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", "$command 2>/dev/null"),
                null,
                null  // dir 参数为 null
            ) as ShizukuRemoteProcess
            
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (output.isNotEmpty()) output.append("\n")
                output.append(line)
            }
            
            // 等待进程结束并检查错误
            val exitCode = process.waitFor()
            
            // 读取错误输出
            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                if (errorOutput.isNotEmpty()) errorOutput.append("\n")
                errorOutput.append(line)
            }
            
            reader.close()
            errorReader.close()
            process.destroy()
            
            if (exitCode != 0 && errorOutput.isNotEmpty()) {
                Log.w(TAG, "execCommand stderr: $errorOutput")
            }
            
            val result = output.toString()
            Log.d(TAG, "execCommand: '$command' -> exitCode=$exitCode, outputLength=${result.length}")
            result.ifEmpty { null }
        } catch (e: Exception) {
            Log.e(TAG, "execCommand failed: ${e.message}", e)
            // 打印完整的异常堆栈帮助调试
            e.printStackTrace()
            null
        }
    }

    data class CommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )

    fun execCommandWithInput(command: String, input: InputStream): CommandResult? {
        return try {
            if (!isShizukuRunning() || checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return null
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            ).apply { isAccessible = true }
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as ShizukuRemoteProcess
            input.use { source -> source.copyTo(process.outputStream) }
            process.outputStream.close()
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            process.destroy()
            CommandResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "execCommandWithInput failed: ${e.message}", e)
            null
        }
    }

    fun execCommandWithResult(command: String): CommandResult? {
        return try {
            if (!isShizukuRunning() || checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return null
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            ).apply { isAccessible = true }
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as ShizukuRemoteProcess
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            process.destroy()
            CommandResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            Log.e(TAG, "execCommandWithResult failed: ${e.message}", e)
            null
        }
    }

    fun openCommandInputStream(command: String): InputStream? {
        return try {
            if (!isShizukuRunning() || checkSelfPermission() != PackageManager.PERMISSION_GRANTED) return null
            val newProcessMethod: Method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            ).apply { isAccessible = true }
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as ShizukuRemoteProcess
            object : FilterInputStream(process.inputStream) {
                override fun close() {
                    try {
                        super.close()
                        process.errorStream.close()
                        process.waitFor()
                    } finally {
                        process.destroy()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "openCommandInputStream failed: ${e.message}", e)
            null
        }
    }
    
    /**
     * 使用 Shizuku 执行多条命令
     * 宝贝这里支持一次性执行多条命令喵~
     */
    fun execMultipleCommands(commands: List<String>): List<String?> {
        return commands.map { execCommand(it) }
    }
    
    /**
     * 获取当前激活模式
     */
    fun getCurrentActivationMode(): ActivationMode {
        return when {
            // 优先检测 Sui (Magisk 模块)
            isSui() -> ActivationMode.SHIZUKU
            // 检测 Shizuku 运行时
            isShizukuRunning() -> ActivationMode.SHIZUKU
            // 检测 Root 运行时（传统 RootManager 方式）
            RootManager.isRootAvailable() -> ActivationMode.ROOT
            // 默认无特殊权限
            else -> ActivationMode.DEFAULT
        }
    }
    
    /**
     * 检测 Root 运行时是否可用（传统方式）
     * @deprecated 请使用 RootManager.isRootAvailable()
     */
    @Deprecated("请使用 RootManager.isRootAvailable()")
    private fun isRootAvailable(): Boolean {
        return RootManager.isRootAvailable()
    }
}

/**
 * Composable 状态管理 - 监听 Shizuku 状态变化
 */
@Composable
fun rememberShizukuState(): ShizukuState {
    var isRunning by remember { mutableStateOf(ShizukuManager.isShizukuRunning()) }
    var isSui by remember { mutableStateOf(ShizukuManager.isSui()) }
    var permissionGranted by remember { mutableStateOf(ShizukuManager.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) }
    var uid by remember { mutableStateOf(ShizukuManager.getUid()) }
    var version by remember { mutableStateOf(ShizukuManager.getVersion()) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        fun resampleState() {
            isRunning = ShizukuManager.isShizukuRunning()
            isSui = ShizukuManager.isSui()
            permissionGranted = ShizukuManager.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            uid = ShizukuManager.getUid()
            version = ShizukuManager.getVersion()
        }

        val onBinderReceived = {
            resampleState()
        }

        val onBinderDead = {
            isRunning = false
            uid = -1
        }

        val onPermissionResult: (Int, Int) -> Unit = { _, grantResult ->
            permissionGranted = grantResult == PackageManager.PERMISSION_GRANTED
        }

        val binderReceivedListener = ShizukuManager.addBinderReceivedListener(onBinderReceived)
        val binderDeadListener = ShizukuManager.addBinderDeadListener(onBinderDead)
        val permissionListener = ShizukuManager.addPermissionResultListener(onPermissionResult)

        // 在 Shizuku 应用内手动授权不会触发任何回调，授权后回到 App 时借助 ON_RESUME 重采样自愈
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resampleState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ShizukuManager.removeBinderReceivedListener(binderReceivedListener)
            ShizukuManager.removeBinderDeadListener(binderDeadListener)
            ShizukuManager.removePermissionResultListener(permissionListener)
        }
    }

    return ShizukuState(
        isRunning = isRunning,
        isSui = isSui,
        permissionGranted = permissionGranted,
        uid = uid,
        version = version
    )
}

/**
 * Shizuku 状态数据类
 */
data class ShizukuState(
    val isRunning: Boolean,
    val isSui: Boolean,
    val permissionGranted: Boolean,
    val uid: Int,
    val version: Int
) {
    /**
     * 获取运行时类型名称
     */
    fun getRuntimeTypeName(): String {
        return when {
            isSui -> "Sui (Magisk)"
            isRunning -> when (uid) {
                0 -> "Shizuku (Root)"
                2000 -> "Shizuku (ADB)"
                else -> "Shizuku (UID: $uid)"
            }
            else -> "未连接"
        }
    }
    
    /**
     * 获取权限状态描述
     */
    fun getPermissionStatus(): String {
        return when {
            !isRunning -> "Shizuku 未运行"
            permissionGranted -> "已授权"
            else -> "未授权"
        }
    }
}
