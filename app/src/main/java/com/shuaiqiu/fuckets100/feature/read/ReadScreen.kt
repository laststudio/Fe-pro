package com.shuaiqiu.fuckets100.feature.read

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveScaffold
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import com.shuaiqiu.fuckets100.core.designsystem.TopBarBackButton
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets

import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.AppCoroutineScope
import com.shuaiqiu.fuckets100.ChangyanWebLoginActivity
import com.shuaiqiu.fuckets100.CloudContent
import com.shuaiqiu.fuckets100.CloudHomeworkState
import com.shuaiqiu.fuckets100.ETS100AnswerReader
import com.shuaiqiu.fuckets100.ETS100ApiClient
import com.shuaiqiu.fuckets100.ETS100AuthManager
import com.shuaiqiu.fuckets100.ETS100FileReader
import com.shuaiqiu.fuckets100.FeAccountFeatures
import com.shuaiqiu.fuckets100.FeAccountManager
import com.shuaiqiu.fuckets100.core.designsystem.FeFilledCard
import com.shuaiqiu.fuckets100.core.designsystem.FeThinDivider
import com.shuaiqiu.fuckets100.core.designsystem.FeTonalIconContainer
import com.shuaiqiu.fuckets100.PaperSource
import com.shuaiqiu.fuckets100.PaperSourceExporter
import com.shuaiqiu.fuckets100.ResolverAuthorizationDeniedException
import com.shuaiqiu.fuckets100.ResolverAuthorizer
import com.shuaiqiu.fuckets100.SettingsManager
import com.shuaiqiu.fuckets100.ZipPasswordGenerator
import com.shuaiqiu.fuckets100.core.designsystem.FeFilledCard
import com.shuaiqiu.fuckets100.core.designsystem.FeThinDivider
import com.shuaiqiu.fuckets100.core.designsystem.FeTonalIconContainer
private const val TAG = "ReadScreen"

// ============================================================================
// 调试日志系统 - 用于跟踪应用内数据和答案的读取过程
// ============================================================================

/**
 * 日志级别枚举
 * 定义日志的重要程度和显示颜色
 */
private enum class LogLevel(val label: String, val colorHex: Long) {
    DEBUG("调试", 0xFF60A5FA),      // 蓝色 - 调试信息
    INFO("信息", 0xFFFFD93D),       // 黄色 - 一般信息
    SUCCESS("成功", 0xFF2DD4BF),    // 青色 - 成功状态
    WARN("警告", 0xFFFB923C),       // 橙色 - 警告信息
    ERROR("错误", 0xFFFF6B6B),      // 红色 - 错误信息
    INIT("初始化", 0xFFE879F9)      // 紫色 - 初始化相关
}

/**
 * 日志类别枚举
 * 用于分组显示不同类型的数据
 */
private enum class LogCategory(val label: String) {
    SYSTEM("系统"),
    FILE("文件"),
    PAPER("试卷"),
    SECTION("分区"),
    QUESTION("题目"),
    ANSWER("答案")
}

/**
 * 初始化结果密封类
 * 用于安全地在 IO 线程和主线程之间传递数据
 */
private sealed class InitResult {
    data class Success(
        val readerInfo: String,
        val dataFiles: List<ETS100FileReader.FileItem>,
        val resourceFiles: List<ETS100FileReader.FileItem>,
        val papers: List<ETS100AnswerReader.Paper>
    ) : InitResult()
    
    data class Error(val message: String) : InitResult()
}

private enum class LocalParsePhase {
    IDLE,
    SCANNING,
    PARSING,
    COMPLETED,
    FAILED
}

private object LocalReadProgressState {
    var phase by mutableStateOf(LocalParsePhase.IDLE)
    var doneCount by mutableIntStateOf(0)
    var totalCount by mutableIntStateOf(0)
    var sectionCount by mutableIntStateOf(0)
    var questionCount by mutableIntStateOf(0)
    var parsedGroupIndexes by mutableStateOf(emptySet<Int>())
    var message by mutableStateOf("")
    var parseJob: Job? = null

    fun reset() {
        parseJob?.cancel()
        parseJob = null
        phase = LocalParsePhase.IDLE
        doneCount = 0
        totalCount = 0
        sectionCount = 0
        questionCount = 0
        parsedGroupIndexes = emptySet()
        message = ""
    }
}

/**
 * 阅读界面 - 显示ETS 100答案的阅读界面
 * 支持多种激活模式（Shizuku、Root、SAF等）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadScreen(
    currentMode: ActivationMode,
    accountOnline: Boolean = true,
    readAccessChecking: Boolean = false,
    readAccessIssue: String? = null,
    onNavigateToActivation: () -> Unit,
    onOpenPaperDetail: (ETS100AnswerReader.Paper) -> Unit,
    compactAnswerDisplay: Boolean = SettingsManager.getCompactAnswerDisplay()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cachedLocalSnapshot = remember(currentMode) {
        ReadPageStateStore.loadLocal(context)?.takeIf { it.mode == currentMode }
    }
    val cachedCloudSnapshot = remember {
        ReadPageStateStore.loadCloud(context)?.also { snapshot ->
            if (CloudHomeworkState.homeworkListsByStatus.isEmpty() &&
                CloudHomeworkState.downloadedPapers.isEmpty()
            ) {
                val validDownloadedPapers = snapshot.downloadedPapers.filterValues { papers ->
                    papers.isNotEmpty() && papers.all { it.source is PaperSource.Cloud }
                }
                CloudHomeworkState.selectedStatus = snapshot.selectedStatus
                CloudHomeworkState.selectedCategory = snapshot.selectedCategory
                CloudHomeworkState.homeworkListsByStatus = snapshot.homeworkListsByStatus
                CloudHomeworkState.cloudBaseUrl = snapshot.cloudBaseUrl
                CloudHomeworkState.downloadedPapers = validDownloadedPapers
                CloudHomeworkState.downloadedHomeworkNames =
                    snapshot.downloadedHomeworkNames.intersect(validDownloadedPapers.keys)
                CloudHomeworkState.failedCloudHomeworks = snapshot.failedCloudHomeworks
            }
        }
    }

    fun openPaperDetail(paper: ETS100AnswerReader.Paper) {
        onOpenPaperDetail(paper)
    }

    fun copyPaperText(paper: ETS100AnswerReader.Paper) {
        val (available, reason) = FeAccountManager.get(context).isFeatureAvailable(FeAccountFeatures.ANSWER_SHARE)
        if (!available) {
            android.widget.Toast.makeText(context, reason ?: "答案分享功能当前不可用", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val text = formatPaperAsText(paper)
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(paper.title, text))
        android.widget.Toast.makeText(context, "已复制答案到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    // 文件列表状态
    var dataFiles by remember {
        mutableStateOf(if (currentMode != ActivationMode.CLOUD) cachedLocalSnapshot?.dataFiles.orEmpty() else emptyList())
    }
    var resourceFiles by remember {
        mutableStateOf(if (currentMode != ActivationMode.CLOUD) cachedLocalSnapshot?.resourceFiles.orEmpty() else emptyList())
    }
    var readerInfo by remember {
        mutableStateOf(if (currentMode != ActivationMode.CLOUD) cachedLocalSnapshot?.readerInfo.orEmpty() else "")
    }
    
    // 试卷和题目状态
    var papers by remember {
        mutableStateOf(if (currentMode != ActivationMode.CLOUD) cachedLocalSnapshot?.papers.orEmpty() else emptyList())
    }
    var selectedPaper by remember { mutableStateOf<ETS100AnswerReader.Paper?>(null) }
    var selectedSectionIndex by remember { mutableIntStateOf(-1) }
    var selectedQuestionIndex by remember { mutableIntStateOf(-1) }
    
    // 二级页面导航状态 - 宝贝用于切换试卷详情页面喵~
    var showPaperDetail by remember { mutableStateOf(false) }
    
    // 加载状态
    var isLoading by remember {
        mutableStateOf(currentMode != ActivationMode.CLOUD && cachedLocalSnapshot == null)
    }
    var isParsingLocalAnswers by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var sourceExportPaper by remember { mutableStateOf<ETS100AnswerReader.Paper?>(null) }
    var showLocalDirectoryExport by remember { mutableStateOf(false) }
    var showCloudDirectoryExport by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableIntStateOf(0) }  // 宝贝添加了重新加载触发器喵~
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }  // 宝贝添加了删除确认对话框状态喵~
    
    // 展开状态 - 宝贝现在使用 PaperListItem，不需要这些展开状态了喵~
    // var expandedPapers by remember { mutableStateOf(setOf<Int>()) }
    // var expandedSections by remember { mutableStateOf(setOf<Pair<Int, Int>>()) }
    // 搜索相关状态 - 已移除喵~
    // var questionSearchQuery by remember { mutableStateOf("") }
    // var showOnlyAnswered by remember { mutableStateOf(false) }
    
    // 可展开 FAB 相关状态
    var showCloudReadConfirmDialog by remember { mutableStateOf(false) }
    var showReadActionsMenu by remember { mutableStateOf(false) }
    val paperListState = key(currentMode) { rememberLazyListState() }
    val readFabExpanded by remember(paperListState) {
        derivedStateOf {
            paperListState.firstVisibleItemIndex == 0 && paperListState.firstVisibleItemScrollOffset < 100
        }
    }

    var selectedCloudCategory by remember { mutableStateOf(CloudHomeworkState.selectedCategory) }
    var selectedCloudHomeworkStatus by remember { mutableStateOf(CloudHomeworkState.selectedStatus) }
    var homeworkListsByStatus by remember { mutableStateOf(CloudHomeworkState.homeworkListsByStatus) }
    var isLoadingCloudHomework by remember { mutableStateOf(CloudHomeworkState.isLoading) }
    var cloudHomeworkError by remember { mutableStateOf(CloudHomeworkState.error) }
    var changyanTokenExpired by remember { mutableStateOf(false) }
    var cloudBaseUrl by remember { mutableStateOf(CloudHomeworkState.cloudBaseUrl) }
    var downloadedPapers by remember { mutableStateOf(CloudHomeworkState.downloadedPapers) }
    var downloadedHomeworkNames by remember { mutableStateOf(CloudHomeworkState.downloadedHomeworkNames) }
    var cloudDownloadingHomeworks by remember { mutableStateOf(CloudHomeworkState.cloudDownloadingHomeworks) }
    var cloudDownloadProgress by remember { mutableStateOf(CloudHomeworkState.cloudDownloadProgress) }
    var failedCloudHomeworks by remember { mutableStateOf(CloudHomeworkState.failedCloudHomeworks) }

    // 宝贝新增：云端作业占位符列表喵~ 支持当前、历史及词汇PK三类视图
    val homeworkList by remember {
        derivedStateOf {
            when (selectedCloudCategory) {
                CloudHomeworkState.CATEGORY_CURRENT ->
                    homeworkListsByStatus[CloudHomeworkState.STATUS_CURRENT].orEmpty()
                CloudHomeworkState.CATEGORY_HISTORY ->
                    homeworkListsByStatus[CloudHomeworkState.STATUS_HISTORY].orEmpty()
                CloudHomeworkState.CATEGORY_VOCAB_PK -> {
                    val allLoaded = (homeworkListsByStatus[CloudHomeworkState.STATUS_CURRENT].orEmpty() +
                        homeworkListsByStatus[CloudHomeworkState.STATUS_HISTORY].orEmpty())
                        .distinctBy { it.id.ifBlank { it.name } }
                    allLoaded.filter { it.pk == 1 || it.setType == 6 }
                }
                else -> homeworkListsByStatus[CloudHomeworkState.STATUS_CURRENT].orEmpty()
            }
        }
    }
    val selectedCloudHomeworkLabel by remember {
        derivedStateOf {
            when (selectedCloudCategory) {
                CloudHomeworkState.CATEGORY_CURRENT -> "当前作业"
                CloudHomeworkState.CATEGORY_HISTORY -> "历史作业"
                CloudHomeworkState.CATEGORY_VOCAB_PK -> "词汇PK"
                else -> "当前作业"
            }
        }
    }
    val isCurrentCategoryLoaded by remember {
        derivedStateOf {
            when (selectedCloudCategory) {
                CloudHomeworkState.CATEGORY_CURRENT ->
                    homeworkListsByStatus.containsKey(CloudHomeworkState.STATUS_CURRENT)
                CloudHomeworkState.CATEGORY_HISTORY ->
                    homeworkListsByStatus.containsKey(CloudHomeworkState.STATUS_HISTORY)
                CloudHomeworkState.CATEGORY_VOCAB_PK ->
                    homeworkListsByStatus.containsKey(CloudHomeworkState.STATUS_CURRENT) ||
                    homeworkListsByStatus.containsKey(CloudHomeworkState.STATUS_HISTORY)
                else -> false
            }
        }
    }
    val cloudHomeworkPlaceholders by remember {
        derivedStateOf {
            homeworkList.map { hw -> createCloudHomeworkPlaceholder(hw) }
        }
    }
    val loadedLocalSectionCount by remember { derivedStateOf { papers.totalSectionCount() } }
    val loadedLocalQuestionCount by remember { derivedStateOf { papers.totalQuestionCount() } }
    val downloadedCloudPapers by remember {
        derivedStateOf { downloadedPapers.values.flatten() }
    }
    val downloadedCloudQuestionCount by remember {
        derivedStateOf { downloadedCloudPapers.totalQuestionCount() }
    }
    val shouldOpenChangyanLoginDirectly by remember {
        derivedStateOf {
            currentMode == ActivationMode.CLOUD &&
                cloudHomeworkError != null &&
                ETS100AuthManager.isChangyanWebLogin(context)
        }
    }
    
    val categoryColors = answerCategoryColors()
    
    // Diagnostics remain in Logcat without retaining an unbounded UI history.
    fun addLog(
        level: LogLevel = LogLevel.INFO,
        category: LogCategory = LogCategory.SYSTEM,
        message: String
    ) {
        val logcatMessage = "[${category.label}] $message"
        when (level) {
            LogLevel.ERROR -> Log.e(TAG, logcatMessage)
            LogLevel.WARN -> Log.w(TAG, logcatMessage)
            else -> Log.i(TAG, logcatMessage)
        }
    }

    fun saveCloudReadState() {
        ReadPageStateStore.saveCloud(
            context,
            ReadPageStateStore.CloudSnapshot(
                selectedStatus = selectedCloudHomeworkStatus,
                homeworkListsByStatus = homeworkListsByStatus,
                cloudBaseUrl = cloudBaseUrl,
                downloadedPapers = downloadedPapers,
                downloadedHomeworkNames = downloadedHomeworkNames,
                failedCloudHomeworks = failedCloudHomeworks,
                selectedCategory = selectedCloudCategory
            )
        )
    }

    fun updateCloudDownloadProgress(
        cacheKey: String,
        progress: CloudHomeworkState.DownloadProgress?
    ) {
        cloudDownloadProgress = if (progress == null) {
            cloudDownloadProgress - cacheKey
        } else {
            cloudDownloadProgress + (cacheKey to progress)
        }
        CloudHomeworkState.cloudDownloadProgress = cloudDownloadProgress
    }

    // ========== 云端模式辅助函数喵~ ==========

    /**
     * 加载云端作业列表
     */
    suspend fun loadCloudHomeworkList(status: String = selectedCloudHomeworkStatus) {
        if (isLoadingCloudHomework) return

        // 先进入加载态，再执行授权校验和账号获取，避免点击后网络操作期间页面像卡死。
        isLoadingCloudHomework = true
        cloudHomeworkError = null
        changyanTokenExpired = false
        CloudHomeworkState.isLoading = true
        CloudHomeworkState.error = null

        fun stopCloudLoading() {
            isLoadingCloudHomework = false
            CloudHomeworkState.isLoading = false
        }

        if (!accountOnline) {
            cloudHomeworkError = "已离线"
            stopCloudLoading()
            return
        }
        try {
            ResolverAuthorizer.requireCloudAuthorized(context)
        } catch (e: ResolverAuthorizationDeniedException) {
            cloudHomeworkError = e.message ?: "云端模式当前不可用"
            CloudHomeworkState.error = cloudHomeworkError
            stopCloudLoading()
            return
        }
        selectedCloudHomeworkStatus = status
        CloudHomeworkState.selectedStatus = status

        val isChangyanLogin = ETS100AuthManager.isChangyanWebLogin(context)
        val savedToken: String
        var parentId: String
        var candidateAccounts: List<JSONObject> = emptyList()
        if (isChangyanLogin) {
            savedToken = ETS100AuthManager.getToken(context).orEmpty()
            parentId = ETS100AuthManager.getParentAccountId(context).orEmpty()
            if (savedToken.isEmpty() || parentId.isEmpty()) {
                cloudHomeworkError = "讯飞登录信息不完整，请重新登录"
                addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 讯飞登录信息不完整")
                stopCloudLoading()
                return
            }
        } else {
            try {
                val cloudSession = FeAccountManager.get(context).cloudSession(
                    ETS100AuthManager.getSelectedEcardId(context).orEmpty()
                )
                ETS100AuthManager.saveDeviceCode(context, cloudSession.deviceCode)
                ETS100AuthManager.saveLoginInfo(
                    context,
                    cloudSession.accountDisplay,
                    cloudSession.token,
                    cloudSession.parentAccountId,
                )
                ETS100AuthManager.saveLoginMethod(
                    context,
                    ETS100AuthManager.LOGIN_METHOD_BACKEND_BOUND,
                )
                ETS100AuthManager.clearPassword(context)
                val account = cloudSession.selectedAccount
                ETS100AuthManager.saveSelectedEcard(
                    context,
                    account.optString("id"),
                    account.optString("name"),
                    account.optString("grade"),
                    account.optString("class_id"),
                )
                savedToken = cloudSession.token
                parentId = cloudSession.parentAccountId
                candidateAccounts = cloudSession.validAccounts
            } catch (e: Exception) {
                cloudHomeworkError = e.message ?: "无法读取 LA Studio 绑定账号"
                CloudHomeworkState.error = cloudHomeworkError
                addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 后端绑定账号不可用: ${e.message}")
                stopCloudLoading()
                return
            }
        }

        addLog(LogLevel.INFO, LogCategory.SYSTEM, "☁️ 开始加载${cloudHomeworkStatusLabel(status)}列表")

        val requestStatuses = cloudHomeworkRequestStatuses(status)
        var lastError: Throwable? = null
        var success = false

        suspend fun requestHomeworkLists(tokenForRequest: String): List<ETS100ApiClient.HomeworkListResponse> {
            return coroutineScope {
                requestStatuses.map { requestStatus ->
                    async {
                        var res = ETS100ApiClient.getHomeworkList(tokenForRequest, parentId, requestStatus)
                        val errMsg = res.exceptionOrNull()?.message.orEmpty()
                        val isClassError = errMsg.contains("80004") || errMsg.contains("有效班级")

                        // 若当前 parent_id 报 80004 (没有找到有效班级)，自动轮询候选账号 (与网页端 homework.py 保持一致)
                        if (res.isFailure && isClassError && candidateAccounts.isNotEmpty()) {
                            for (candidate in candidateAccounts) {
                                val candidatePid = candidate.optString("parent_id")
                                if (candidatePid.isBlank() || candidatePid == parentId) continue
                                val fallbackRes = ETS100ApiClient.getHomeworkList(tokenForRequest, candidatePid, requestStatus)
                                if (fallbackRes.isSuccess) {
                                    val candidateLabel = candidate.optString("name").ifBlank { candidate.optString("grade") }
                                    addLog(
                                        LogLevel.INFO,
                                        LogCategory.SYSTEM,
                                        "✓ 当前班级已失效，已自动回退至候选账号 ($candidateLabel, parent_id=$candidatePid) 读取成功"
                                    )
                                    parentId = candidatePid
                                    ETS100AuthManager.saveParentAccountId(context, candidatePid)
                                    ETS100AuthManager.saveSelectedEcard(
                                        context,
                                        candidate.optString("id"),
                                        candidate.optString("name"),
                                        candidate.optString("grade"),
                                        candidate.optString("class_id"),
                                    )
                                    res = fallbackRes
                                    break
                                }
                            }
                        }
                        requestStatus to res
                    }
                }.awaitAll().mapNotNull { (requestStatus, result) ->
                    result.onSuccess { response ->
                        addLog(LogLevel.SUCCESS, LogCategory.SYSTEM, "✓ status=$requestStatus 获取到 ${response.homeworks.size} 个作业")
                        addLog(LogLevel.INFO, LogCategory.SYSTEM, "📡 API 返回的 base_url = ${response.baseUrl}")
                        response.homeworks.forEachIndexed { index, homework ->
                            addLog(
                                LogLevel.DEBUG,
                                LogCategory.PAPER,
                                "   ├─ 作业[$index] id=${homework.id.ifBlank { "<empty>" }}, " +
                                    "name=${homework.name}, contents=${homework.contents.size}"
                            )
                            homework.contents.forEachIndexed { contentIndex, content ->
                                addLog(
                                    LogLevel.DEBUG,
                                    LogCategory.PAPER,
                                    "   │  ├─ content[$contentIndex] group=${content.groupName}, url=${content.url}"
                                )
                            }
                        }
                    }.onFailure { e ->
                        lastError = e
                        val isClassError = e.message?.contains("80004") == true || e.message?.contains("有效班级") == true
                        val displayMsg = if (isClassError && status == CloudHomeworkState.STATUS_CURRENT) {
                            "当前账号无有效班级或新作业，可切换至上方「历史作业」查看已归档的作业与答案"
                        } else {
                            e.message
                        }
                        addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ status=$requestStatus 获取作业列表失败: $displayMsg")
                    }.getOrNull()
                }
            }
        }

        addLog(LogLevel.INFO, LogCategory.SYSTEM, "🔐 使用已保存 Token 请求作业列表")
        var responses = requestHomeworkLists(savedToken)

        if (responses.isEmpty()) {
            if (!isChangyanLogin) {
                try {
                    val cloudSession = FeAccountManager.get(context).cloudSession(
                        ETS100AuthManager.getSelectedEcardId(context).orEmpty()
                    )
                    ETS100AuthManager.saveDeviceCode(context, cloudSession.deviceCode)
                    ETS100AuthManager.saveLoginInfo(
                        context,
                        cloudSession.accountDisplay,
                        cloudSession.token,
                        cloudSession.parentAccountId,
                    )
                    parentId = cloudSession.parentAccountId
                    candidateAccounts = cloudSession.validAccounts
                    ETS100AuthManager.saveLoginMethod(
                        context,
                        ETS100AuthManager.LOGIN_METHOD_BACKEND_BOUND,
                    )
                    ETS100AuthManager.clearPassword(context)
                    val account = cloudSession.selectedAccount
                    ETS100AuthManager.saveSelectedEcard(
                        context,
                        account.optString("id"),
                        account.optString("name"),
                        account.optString("grade"),
                        account.optString("class_id"),
                    )
                    addLog(LogLevel.INFO, LogCategory.SYSTEM, "✓ 已从 LA Studio 后端刷新绑定账号会话")
                    responses = requestHomeworkLists(cloudSession.token)
                } catch (e: Exception) {
                    lastError = e
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 后端绑定账号会话刷新失败: ${e.message}")
                }
                if (responses.isEmpty()) {
                    val isClassError = lastError?.message?.let { it.contains("80004") || it.contains("有效班级") } == true
                    cloudHomeworkError = if (isClassError && status == CloudHomeworkState.STATUS_CURRENT) {
                        "当前账号无有效班级，可切换至「历史作业」查看往期归档答案喵~"
                    } else {
                        lastError?.message ?: "后端绑定账号不可用"
                    }
                    CloudHomeworkState.error = cloudHomeworkError
                    isLoadingCloudHomework = false
                    CloudHomeworkState.isLoading = false
                    return
                }
            } else {
                val login = ETS100AuthManager.getPhone(context).orEmpty()
                cloudHomeworkError = "企业账号 Token 已失效，请重新进行讯飞登录"
                changyanTokenExpired = true
                CloudHomeworkState.error = cloudHomeworkError
                addLog(
                    LogLevel.WARN,
                    LogCategory.SYSTEM,
                    "企业登录 Token 请求失败，不执行账号密码重登；login=$login，保留已选账号上下文，等待重新讯飞登录"
                )
                isLoadingCloudHomework = false
                CloudHomeworkState.isLoading = false
                return
            }
        }

        val mergedHomeworks = responses.flatMap { it.homeworks }
        val latestBaseUrl = responses.lastOrNull()?.baseUrl ?: cloudBaseUrl
        val hasAnySuccessfulRequest = responses.isNotEmpty()

        if (hasAnySuccessfulRequest) {
            changyanTokenExpired = false
            homeworkListsByStatus = homeworkListsByStatus + (status to mergedHomeworks)
            cloudBaseUrl = latestBaseUrl
            CloudHomeworkState.homeworkListsByStatus = homeworkListsByStatus
            CloudHomeworkState.cloudBaseUrl = latestBaseUrl
            saveCloudReadState()
            addLog(LogLevel.SUCCESS, LogCategory.SYSTEM, "✓ 合并得到 ${mergedHomeworks.size} 个${cloudHomeworkStatusLabel(status)}")
            mergedHomeworks.forEachIndexed { index, homework ->
                addLog(
                    LogLevel.INFO,
                    LogCategory.PAPER,
                    "📚 合并作业[$index]: id=${homework.id.ifBlank { "<empty>" }}, " +
                        "name=${homework.name}, contents=${homework.contents.size}, " +
                        "key=${cloudHomeworkCacheKey(status, homework)}"
                )
            }
            success = true
        }

        if (!success) {
            cloudHomeworkError = lastError?.message ?: "获取作业列表失败"
            CloudHomeworkState.error = cloudHomeworkError
            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 获取作业列表失败: $cloudHomeworkError")
        }

        isLoadingCloudHomework = false
        CloudHomeworkState.isLoading = false
    }

    fun selectCloudCategory(category: String) {
        if (category == selectedCloudCategory) return

        selectedCloudCategory = category
        CloudHomeworkState.selectedCategory = category

        val targetStatus = when (category) {
            CloudHomeworkState.CATEGORY_HISTORY -> CloudHomeworkState.STATUS_HISTORY
            else -> CloudHomeworkState.STATUS_CURRENT
        }
        selectedCloudHomeworkStatus = targetStatus
        CloudHomeworkState.selectedStatus = targetStatus
        selectedPaper = null
        showPaperDetail = false
        // 分类切换只切换当前视图，不自动发起网络读取；由用户点击“读取作业”主动加载。
        cloudHomeworkError = null
        CloudHomeworkState.error = null
        saveCloudReadState()
    }

    fun selectCloudHomeworkStatus(status: String) {
        val category = when (status) {
            CloudHomeworkState.STATUS_HISTORY -> CloudHomeworkState.CATEGORY_HISTORY
            else -> CloudHomeworkState.CATEGORY_CURRENT
        }
        selectCloudCategory(category)
    }

    /**
     * 清除云端作业缓存
     * 宝贝删除所有已下载的云端作业文件喵~
     */
    fun clearCloudHomeworkCache() {
        if (isDeleting) return
        isDeleting = true
        scope.launch {
            try {
                addLog(LogLevel.INFO, LogCategory.SYSTEM, "🗑️ 开始后台清除云端缓存")
                withContext(Dispatchers.IO) {
                    val cacheDir = File(context.cacheDir, "cloud_homework")
                    if (cacheDir.exists()) {
                        cacheDir.deleteRecursively()
                    }
                }
                addLog(LogLevel.INFO, LogCategory.SYSTEM, "🗑️ 已删除云端缓存目录")
                downloadedPapers = emptyMap()
                downloadedHomeworkNames = emptySet()
                cloudDownloadingHomeworks = emptySet()
                cloudDownloadProgress = emptyMap()
                failedCloudHomeworks = emptySet()
                papers = emptyList()
                homeworkListsByStatus = emptyMap()
                selectedCloudCategory = CloudHomeworkState.CATEGORY_CURRENT
                selectedCloudHomeworkStatus = CloudHomeworkState.STATUS_CURRENT
                CloudHomeworkState.clear()
                ReadPageStateStore.clearCloud(context)
                Toast.makeText(context, "已清除所有云端缓存", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 清除云端缓存失败: ${e.message}")
                Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isDeleting = false
            }
        }
    }

    /**
     * 下载并解析云端作业
     * 宝贝这个函数处理下载、解压和解析的全流程喵~
     */
    suspend fun downloadAndParseHomework(
        homeworkInfo: ETS100ApiClient.HomeworkInfo,
        status: String = selectedCloudHomeworkStatus
    ) {
        val targetIdentity = cloudHomeworkIdentity(homeworkInfo)
        val cacheKey = cloudHomeworkCacheKey(status, homeworkInfo)
        val currentKey = cloudHomeworkCacheKey(CloudHomeworkState.STATUS_CURRENT, homeworkInfo)
        val historyKey = cloudHomeworkCacheKey(CloudHomeworkState.STATUS_HISTORY, homeworkInfo)

        // 1. 检查是否已经存在有效解析数据（sections 非空），如果存在直接打开并返回
        val existingPaper = downloadedPapers[cacheKey]?.firstOrNull { it.sections.isNotEmpty() }
            ?: downloadedPapers[currentKey]?.firstOrNull { it.sections.isNotEmpty() }
            ?: downloadedPapers[historyKey]?.firstOrNull { it.sections.isNotEmpty() }
            ?: downloadedPapers.entries.firstOrNull { (k, list) ->
                (k.endsWith(":$targetIdentity") ||
                 list.firstOrNull()?.source.let { it is PaperSource.Cloud && it.homeworkIdentity == targetIdentity }) &&
                list.firstOrNull()?.sections?.isNotEmpty() == true
            }?.value?.firstOrNull()

        if (existingPaper != null) {
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "📁 缓存命中，作业已解析喵: ${homeworkInfo.name}")
            cloudDownloadingHomeworks = cloudDownloadingHomeworks - cacheKey - currentKey - historyKey
            CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
            cloudDownloadProgress = cloudDownloadProgress - cacheKey - currentKey - historyKey
            CloudHomeworkState.cloudDownloadProgress = cloudDownloadProgress
            if (!downloadedHomeworkNames.contains(cacheKey)) {
                downloadedHomeworkNames = downloadedHomeworkNames + cacheKey
                CloudHomeworkState.downloadedHomeworkNames = downloadedHomeworkNames
            }
            openPaperDetail(existingPaper)
            return
        }

        if (!accountOnline) {
            cloudDownloadingHomeworks = cloudDownloadingHomeworks - cacheKey
            CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
            Toast.makeText(context, "已离线", Toast.LENGTH_SHORT).show()
            return
        }

        // 确保进入下载中状态并显示授权校验进度
        cloudDownloadingHomeworks = cloudDownloadingHomeworks + cacheKey
        failedCloudHomeworks = failedCloudHomeworks - cacheKey
        downloadedPapers = downloadedPapers - cacheKey
        CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
        CloudHomeworkState.failedCloudHomeworks = failedCloudHomeworks
        CloudHomeworkState.downloadedPapers = downloadedPapers
        updateCloudDownloadProgress(
            cacheKey,
            CloudHomeworkState.DownloadProgress(currentFileName = "正在校验授权...")
        )

        try {
            // 并发执行授权校验以消除串行网络等待
            coroutineScope {
                val cloudAuth = async { ResolverAuthorizer.requireCloudAuthorized(context) }
                val resolverAuth = async { ResolverAuthorizer.requireAuthorized(context) }
                cloudAuth.await()
                resolverAuth.await()
            }
        } catch (e: ResolverAuthorizationDeniedException) {
            cloudDownloadingHomeworks = cloudDownloadingHomeworks - cacheKey
            CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
            cloudDownloadProgress = cloudDownloadProgress - cacheKey
            CloudHomeworkState.cloudDownloadProgress = cloudDownloadProgress
            val message = e.message ?: "答案解析功能当前不可用"
            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ $message")
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            return
        } catch (e: Exception) {
            cloudDownloadingHomeworks = cloudDownloadingHomeworks - cacheKey
            CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
            cloudDownloadProgress = cloudDownloadProgress - cacheKey
            CloudHomeworkState.cloudDownloadProgress = cloudDownloadProgress
            val message = e.message ?: "授权校验失败"
            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ $message")
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            return
        }
        addLog(LogLevel.INFO, LogCategory.SYSTEM, "📥 开始下载作业: ${homeworkInfo.name}, 共 ${homeworkInfo.contents.size} 个内容")
        updateCloudDownloadProgress(
            cacheKey,
            CloudHomeworkState.DownloadProgress(currentFileName = homeworkInfo.name)
        )

        var sourceOnlyPaper: ETS100AnswerReader.Paper? = null
        var cacheDir: File? = null
        try {
            val source = PaperSource.Cloud(
                status = status,
                homeworkIdentity = cloudHomeworkIdentity(homeworkInfo),
                baseUrl = cloudBaseUrl,
                contents = homeworkInfo.contents.map { CloudContent(it.groupName, it.url) },
            )
            sourceOnlyPaper = ETS100AnswerReader.Paper(
                paperId = cacheKey.hashCode().toLong(),
                title = homeworkInfo.name,
                dataFileName = cacheKey,
                fileSize = 0L,
                sections = emptyList(),
                source = source,
            )
            cacheDir = PaperSourceExporter.cloudCacheDirectory(
                context,
                status,
                cloudHomeworkIdentity(homeworkInfo),
                cloudBaseUrl,
                homeworkInfo.contents.map { CloudContent(it.groupName, it.url) }
            ).apply { mkdirs() }
            val allSections = mutableListOf<ETS100AnswerReader.Section>()
            var questionIndex = 0

            val isPkOrVocab = homeworkInfo.contents.isEmpty() || homeworkInfo.pk == 1 || homeworkInfo.setType == 6
            if (isPkOrVocab) {
                addLog(LogLevel.INFO, LogCategory.SYSTEM, "🌐 检测到 PK/词汇在线作业，正在从云端获取题目数据...")
                val token = ETS100AuthManager.getToken(context).orEmpty()
                val parentId = ETS100AuthManager.getParentAccountId(context).orEmpty()

                var pkJson: org.json.JSONObject? = null
                fun hasQuestions(json: org.json.JSONObject?): Boolean {
                    if (json == null || json.length() == 0) return false
                    return (json.optJSONArray("word_question")?.length() ?: 0) > 0
                        || (json.optJSONArray("word_questions")?.length() ?: 0) > 0
                        || (json.optJSONArray("questions")?.length() ?: 0) > 0
                        || (json.optJSONArray("data")?.length() ?: 0) > 0
                        || (json.optJSONArray("words")?.length() ?: 0) > 0
                        || (json.optJSONArray("word")?.length() ?: 0) > 0
                }

                if (token.isNotBlank()) {
                    // 路径 1: pk-info
                    val pkResult = ETS100ApiClient.getPkInfo(token, homeworkInfo.id, parentId)
                    val rawPk = pkResult.getOrNull()
                    if (hasQuestions(rawPk)) {
                        pkJson = rawPk
                        addLog(LogLevel.INFO, LogCategory.SYSTEM, "✅ 成功获取 pk-info 响应")
                    } else {
                        val errMsg = pkResult.exceptionOrNull()?.message ?: "无题目"
                        addLog(LogLevel.WARN, LogCategory.SYSTEM, "⚠️ pk-info 未返回题目 ($errMsg)，尝试 homework/struct...")
                        
                        // 路径 2: homework/struct (带 resourceId)
                        val structResult = ETS100ApiClient.getHomeworkStruct(token, homeworkInfo.id, homeworkInfo.resourceId, parentId)
                        val rawStruct = structResult.getOrNull()
                        if (hasQuestions(rawStruct)) {
                            pkJson = rawStruct
                            addLog(LogLevel.INFO, LogCategory.SYSTEM, "✅ 成功从 homework/struct 获取题目数据")
                        } else if (homeworkInfo.resourceId.isNotBlank()) {
                            // 路径 3: homework/struct (不带 resourceId)
                            addLog(LogLevel.WARN, LogCategory.SYSTEM, "⚠️ homework/struct(带resourceId) 未返回题目，尝试不带 resourceId...")
                            val structNoRes = ETS100ApiClient.getHomeworkStruct(token, homeworkInfo.id, "", parentId)
                            val rawStruct2 = structNoRes.getOrNull()
                            if (hasQuestions(rawStruct2)) {
                                pkJson = rawStruct2
                                addLog(LogLevel.INFO, LogCategory.SYSTEM, "✅ 成功从 homework/struct(无resourceId) 获取题目数据")
                            }
                        }

                        // 路径 4: score-detail (已完成历史作业回退)
                        if (pkJson == null) {
                            addLog(LogLevel.WARN, LogCategory.SYSTEM, "⚠️ struct 未返回题目，尝试 score-detail 历史作业解析...")
                            val detailResult = ETS100ApiClient.getScoreDetail(token, homeworkInfo.id, homeworkInfo.resourceId, parentId)
                            val detailBody = detailResult.getOrNull()
                            val detailsArr = detailBody?.optJSONArray("details")
                            if (detailsArr != null && detailsArr.length() > 0) {
                                val wordIdSet = mutableSetOf<Int>()
                                val questionIdSet = mutableSetOf<Int>()
                                for (i in 0 until detailsArr.length()) {
                                    val d = detailsArr.optJSONObject(i) ?: continue
                                    val wArr = d.optJSONArray("word") ?: continue
                                    for (j in 0 until wArr.length()) {
                                        val entry = wArr.optJSONObject(j) ?: continue
                                        val wid = entry.optInt("word_id", 0)
                                        val qid = entry.optInt("question_id", 0)
                                        if (wid > 0) wordIdSet.add(wid)
                                        if (qid > 0) questionIdSet.add(qid)
                                    }
                                }
                                if (wordIdSet.isNotEmpty()) {
                                    addLog(LogLevel.INFO, LogCategory.SYSTEM, "🔍 score-detail 发现 ${wordIdSet.size} 个单词，调用 word/struct...")
                                    val wordStructRes = ETS100ApiClient.getWordStruct(token, wordIdSet.toList())
                                    val wordStructBody = wordStructRes.getOrNull()
                                    val dataArr = wordStructBody?.optJSONArray("data")
                                    if (dataArr != null && dataArr.length() > 0) {
                                        val filteredArr = org.json.JSONArray()
                                        for (k in 0 until dataArr.length()) {
                                            val item = dataArr.optJSONObject(k) ?: continue
                                            val qList = item.optJSONArray("question") ?: continue
                                            val matchingQs = org.json.JSONArray()
                                            for (m in 0 until qList.length()) {
                                                val q = qList.optJSONObject(m) ?: continue
                                                val qid = q.optInt("id", 0)
                                                if (questionIdSet.isEmpty() || questionIdSet.contains(qid)) {
                                                    matchingQs.put(q)
                                                }
                                            }
                                            if (matchingQs.length() > 0) {
                                                val itemCopy = org.json.JSONObject(item.toString())
                                                itemCopy.put("question", matchingQs)
                                                filteredArr.put(itemCopy)
                                            }
                                        }
                                        if (filteredArr.length() > 0) {
                                            pkJson = org.json.JSONObject().put("data", filteredArr)
                                            addLog(LogLevel.INFO, LogCategory.SYSTEM, "✅ 成功从 score-detail + word/struct 恢复 ${filteredArr.length()} 个单词题目")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (pkJson != null && pkJson.length() > 0) {
                    try {
                        val groupName = if (homeworkInfo.setType == 6 || homeworkInfo.pk == 1) "词汇PK" else "在线作业"
                        pkJson.put("structure_type", "word_pk")
                        File(cacheDir, "content.json").writeText(pkJson.toString())
                        val (questions, originalContent) = ETS100AnswerReader.parseContentJson(
                            json = pkJson,
                            startIndex = questionIndex,
                            category = "",
                            typeName = groupName,
                            sectionCaption = groupName,
                            infoJson = "",
                            content2Json = "",
                        )

                        if (questions.isNotEmpty()) {
                            allSections.add(ETS100AnswerReader.Section(
                                caption = groupName,
                                category = pkJson.optString("structure_type", "word.choice"),
                                typeName = groupName,
                                questions = questions,
                                originalContent = originalContent
                            ))
                            questionIndex += questions.size
                            addLog(LogLevel.SUCCESS, LogCategory.QUESTION, "✅ 成功解析 PK/词汇题目 ${questions.size} 道")
                        } else {
                            addLog(LogLevel.WARN, LogCategory.QUESTION, "⚠️ PK/词汇作业未解析出题目")
                        }
                    } catch (e: Exception) {
                        addLog(LogLevel.ERROR, LogCategory.FILE, "✗ 解析 PK/词汇作业失败: ${e.message}")
                    }
                }
            }

            if (allSections.isEmpty()) {
                for ((contentIndex, content) in homeworkInfo.contents.withIndex()) {
                // 1. 构造完整 URL
                val zipUrl = if (content.url.startsWith("http")) {
                    content.url.replaceFirst("http://", "https://")
                } else {
                    var baseUrl = cloudBaseUrl
                    if (baseUrl.startsWith("http://")) {
                        baseUrl = baseUrl.replaceFirst("http://", "https://")
                    }
                    val fullUrl = if (content.url.startsWith("/")) content.url else "/${content.url}"
                    baseUrl.trimEnd('/') + fullUrl
                }
                val zipFileName = zipUrl.substringAfterLast('/').substringBefore('?')
                if (zipFileName.isEmpty()) {
                    addLog(LogLevel.WARN, LogCategory.SYSTEM, "⚠️ 无法从 URL 提取文件名: ${content.url}")
                    continue
                }
                addLog(
                    LogLevel.INFO,
                    LogCategory.SYSTEM,
                    "⬇️ 内容 ${contentIndex + 1}/${homeworkInfo.contents.size}: " +
                        "group=${content.groupName}, url=${content.url}, zip=$zipFileName"
                )

                val zipFile = PaperSourceExporter.cloudCachedFile(cacheDir, content.url, zipFileName)
                if (PaperSourceExporter.isUsableZip(zipFile)) {
                    addLog(LogLevel.INFO, LogCategory.SYSTEM, "📦 文件已存在，跳过下载: ${zipFileName}")
                    updateCloudDownloadProgress(
                        cacheKey,
                        CloudHomeworkState.DownloadProgress(
                            downloadedBytes = zipFile.length(),
                            totalBytes = zipFile.length(),
                            currentFileName = zipFileName
                        )
                    )
                } else {
                    val temporaryFile = File(cacheDir, "${zipFile.name}.part").also { it.delete() }
                    val downloadResult = ETS100ApiClient.downloadFile(zipUrl, temporaryFile) { downloadedBytes, totalBytes ->
                        withContext(Dispatchers.Main) {
                            updateCloudDownloadProgress(
                                cacheKey,
                                CloudHomeworkState.DownloadProgress(
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes,
                                    currentFileName = zipFileName
                                )
                            )
                        }
                    }
                    downloadResult.onFailure { e ->
                        addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 下载失败 ${content.groupName}: ${e.message}")
                    }
                    if (downloadResult.isFailure) {
                        temporaryFile.delete()
                        addLog(LogLevel.WARN, LogCategory.SYSTEM, "⏭️ 跳过失败的下载: ${content.groupName}")
                        continue
                    }
                    if (!PaperSourceExporter.isUsableZip(temporaryFile)) {
                        temporaryFile.delete()
                        addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 下载结果不是完整 ZIP: $zipFileName")
                        continue
                    }
                    if (!temporaryFile.renameTo(zipFile)) {
                        temporaryFile.copyTo(zipFile, overwrite = true)
                        temporaryFile.delete()
                    }
                    addLog(LogLevel.INFO, LogCategory.SYSTEM, "✅ 下载完成: ${zipFile.length()} bytes")
                }

                // 2. 验证 ZIP Magic
                val fis = java.io.FileInputStream(zipFile)
                val magic = ByteArray(4)
                fis.read(magic)
                fis.close()
                val magicHex = magic.joinToString("") { "%02X".format(it) }
                if (magicHex != "504B0304" && magicHex != "504B0506" && magicHex != "504B0708") {
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "⚠️ 文件不是有效 ZIP: $magicHex")
                    continue
                }

                // 3. 生成密码 + 解压
                val password = ZipPasswordGenerator.generatePassword(zipFile.absolutePath)
                if (password == null) {
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 无法生成解压密码: ${zipFileName}")
                    continue
                }

                val extractDirName = zipFileName.removeSuffix(".zip")
                val extractDir = File(cacheDir, extractDirName)
                if (extractDir.exists()) {
                    extractDir.deleteRecursively()
                }
                extractDir.mkdirs()

                try {
                    val zip4jFile = net.lingala.zip4j.ZipFile(zipFile)
                    zip4jFile.setPassword(password.toCharArray())
                    zip4jFile.extractAll(extractDir.absolutePath)
                    addLog(LogLevel.DEBUG, LogCategory.SYSTEM, "📦 解压完成: $extractDirName")
                } catch (e: Exception) {
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 解压失败 ${content.groupName}: ${e.message}")
                    continue
                }

                // 4. 直接从解压目录读取 content.json（不需要找子文件夹）
                addLog(LogLevel.INFO, LogCategory.SYSTEM, "📂 解压目录: ${extractDir.name}")

                // 列出解压目录中的所有文件/文件夹（帮助调试）
                extractDir.listFiles()?.forEach { item ->
                    val type = if (item.isDirectory) "📁" else "📄"
                    addLog(LogLevel.DEBUG, LogCategory.FILE, "  $type ${item.name}")
                }

                val contentJsonFile = File(extractDir, "content.json")
                if (!contentJsonFile.exists()) {
                    addLog(LogLevel.WARN, LogCategory.FILE, "⚠️ 解压目录中无 content.json，跳过")
                } else {
                    val contentJson = contentJsonFile.readText()
                    try {
                        val json = org.json.JSONObject(contentJson)
                        val structureType = json.optString("structure_type", "")
                        val questionCountInJson = json.optJSONObject("info")?.optJSONArray("question")?.length() ?: 0
                        val chooseCountInJson = json.optJSONObject("info")?.optJSONArray("xtlist")?.length() ?: 0
                        val stdCountInJson = json.optJSONObject("info")?.optJSONArray("std")?.length() ?: 0

                        addLog(LogLevel.INFO, LogCategory.SECTION, "📄 解析 content.json")
                        addLog(LogLevel.INFO, LogCategory.SECTION, "   ├─ structure_type: $structureType")
                        addLog(LogLevel.INFO, LogCategory.SECTION, "   ├─ group_name: ${content.groupName}")
                        addLog(
                            LogLevel.INFO,
                            LogCategory.SECTION,
                            "   ├─ raw数量: question=$questionCountInJson, xtlist=$chooseCountInJson, std=$stdCountInJson"
                        )

                        val (questions, originalContent) = ETS100AnswerReader.parseContentJson(
                            json = json,
                            startIndex = questionIndex,
                            category = "",
                            typeName = content.groupName,
                            sectionCaption = content.groupName,
                            infoJson = File(extractDir, "info.json")
                                .takeIf { it.isFile }
                                ?.readText()
                                .orEmpty(),
                            content2Json = File(extractDir, "content2.json")
                                .takeIf { it.isFile }
                                ?.readText()
                                .orEmpty(),
                        )

                        if (questions.isEmpty()) {
                            addLog(LogLevel.WARN, LogCategory.QUESTION, "   └─ ⚠️ 未解析到任何题目！")
                        } else {
                            addLog(
                                LogLevel.SUCCESS,
                                LogCategory.QUESTION,
                                "   ├─ ✅ content ${contentIndex + 1}/${homeworkInfo.contents.size} 解析到 ${questions.size} 道题"
                            )
                            for ((i, q) in questions.withIndex()) {
                                val isLast = i == questions.size - 1
                                val prefix = if (isLast) "   │   └─" else "   │   ├─"
                                addLog(LogLevel.INFO, LogCategory.QUESTION, "$prefix 第${q.order}题: ${q.questionText.take(60)}")
                                if (q.answers.isNotEmpty()) {
                                    for ((j, ans) in q.answers.withIndex()) {
                                        val ansPrefix = if (isLast) "   │       └─" else "   │       ├─"
                                        addLog(LogLevel.INFO, LogCategory.ANSWER, "$ansPrefix 答案${j+1}: ${ans.take(80)}")
                                    }
                                } else {
                                    val ansPrefix = if (isLast) "   │       └─" else "   │       ├─"
                                    addLog(LogLevel.WARN, LogCategory.ANSWER, "$ansPrefix 无标准答案")
                                }
                            }

                            allSections.add(ETS100AnswerReader.Section(
                                caption = content.groupName,
                                category = structureType,
                                typeName = content.groupName,
                                questions = questions,
                                originalContent = originalContent
                            ))
                            questionIndex += questions.size
                            addLog(
                                LogLevel.INFO,
                                LogCategory.SYSTEM,
                                "   └─ 累计: sections=${allSections.size}, questions=$questionIndex"
                            )
                        }
                    } catch (e: Exception) {
                        addLog(LogLevel.ERROR, LogCategory.FILE, "   └─ ✗ 解析 content.json 失败: ${e.message}")
                    }
                }
            }
            }

            if (allSections.isEmpty()) {
                throw Exception("未能解析到任何题目")
            }

            val normalizedSections = normalizeCloudParsedSections(allSections)
            if (normalizedSections.size != allSections.size) {
                addLog(LogLevel.INFO, LogCategory.SECTION, "🔀 已合并云端听后选择分包并按正式题号排序")
            }

            addLog(LogLevel.SUCCESS, LogCategory.SYSTEM, "📝 共解析到 ${normalizedSections.size} 个题型，${questionIndex} 道题")

            val isPk = homeworkInfo.pk == 1 || homeworkInfo.setType == 6
            val paper = ETS100AnswerReader.Paper(
                paperId = cacheKey.hashCode().toLong(),
                title = homeworkInfo.name,
                dataFileName = cacheKey,
                fileSize = 0L,
                sections = normalizedSections,
                downloadTime = System.currentTimeMillis(),
                regionLabel = if (isPk) "词汇PK" else "云端",
                paperName = homeworkInfo.name,
                source = PaperSource.Cloud(
                    status = status,
                    homeworkIdentity = cloudHomeworkIdentity(homeworkInfo),
                    baseUrl = cloudBaseUrl,
                    contents = homeworkInfo.contents.map { CloudContent(it.groupName, it.url) }
                )
            )
            downloadedPapers = downloadedPapers + (cacheKey to listOf(paper))
            downloadedHomeworkNames = downloadedHomeworkNames + cacheKey
            cloudDownloadingHomeworks = cloudDownloadingHomeworks - cacheKey
            cloudDownloadProgress = cloudDownloadProgress - cacheKey
            failedCloudHomeworks = failedCloudHomeworks - cacheKey
            CloudHomeworkState.downloadedPapers = downloadedPapers
            CloudHomeworkState.downloadedHomeworkNames = downloadedHomeworkNames
            CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
            CloudHomeworkState.cloudDownloadProgress = cloudDownloadProgress
            CloudHomeworkState.failedCloudHomeworks = failedCloudHomeworks
            saveCloudReadState()
            // 宝贝下载成功后不再切换页面，只标记已下载，停留作业列表喵~
            Toast.makeText(context, "下载并解析成功！", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 下载解析失败: ${e.message}")
            Toast.makeText(context, "下载或解析失败: ${e.message}", Toast.LENGTH_LONG).show()
            cloudDownloadingHomeworks = cloudDownloadingHomeworks - cacheKey
            cloudDownloadProgress = cloudDownloadProgress - cacheKey
            failedCloudHomeworks = failedCloudHomeworks + cacheKey
            val hasCachedSource = cacheDir?.let { directory ->
                homeworkInfo.contents.any { content ->
                    val name = content.url.substringAfterLast('/').substringBefore('?')
                    name.isNotBlank() && PaperSourceExporter.isUsableZip(
                        PaperSourceExporter.cloudCachedFile(directory, content.url, name),
                    )
                }
            } == true
            if (hasCachedSource) {
                sourceOnlyPaper?.let { paper ->
                    downloadedPapers = downloadedPapers + (cacheKey to listOf(paper))
                }
            }
            CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
            CloudHomeworkState.cloudDownloadProgress = cloudDownloadProgress
            CloudHomeworkState.failedCloudHomeworks = failedCloudHomeworks
            CloudHomeworkState.downloadedPapers = downloadedPapers
            saveCloudReadState()
        } finally {
            if (cloudDownloadingHomeworks.contains(cacheKey)) {
                cloudDownloadingHomeworks = cloudDownloadingHomeworks - cacheKey
                CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
            }
        }
    }

    // 初始化加载 - 使用 rememberUpdatedState 确保最新的 currentMode
    // 宝贝添加了 reloadTrigger 依赖，这样点击读取按钮时就可以重新加载喵~
    val currentModeRef = remember { mutableStateOf(currentMode) }
    LaunchedEffect(currentMode) {
        if (currentMode != ActivationMode.CLOUD) return@LaunchedEffect
        while (true) {
            cloudDownloadingHomeworks = CloudHomeworkState.cloudDownloadingHomeworks
            cloudDownloadProgress = CloudHomeworkState.cloudDownloadProgress
            downloadedPapers = CloudHomeworkState.downloadedPapers
            downloadedHomeworkNames = CloudHomeworkState.downloadedHomeworkNames
            failedCloudHomeworks = CloudHomeworkState.failedCloudHomeworks
            delay(500)
        }
    }

    LaunchedEffect(currentMode, reloadTrigger, accountOnline, readAccessChecking, readAccessIssue) {
        currentModeRef.value = currentMode
        isParsingLocalAnswers = false

        if (readAccessChecking || readAccessIssue != null) {
            isLoading = false
            isParsingLocalAnswers = false
            errorMessage = null
            return@LaunchedEffect
        }

        if (reloadTrigger == 0 &&
            currentMode != ActivationMode.CLOUD &&
            (LocalReadProgressState.phase == LocalParsePhase.SCANNING ||
                LocalReadProgressState.phase == LocalParsePhase.PARSING)
        ) {
            isLoading = false
            isParsingLocalAnswers = true
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "已恢复正在进行的本地答案解析进度")
            return@LaunchedEffect
        }
        
        // 云端模式不执行本地加载，等待用户点击读取按钮喵~
        if (currentMode == ActivationMode.CLOUD) {
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "☁️ 云端模式，等待用户点击读取按钮加载作业列表")
            isLoading = false
            isParsingLocalAnswers = false
            return@LaunchedEffect
        }

        if (reloadTrigger == 0 && cachedLocalSnapshot != null) {
            readerInfo = cachedLocalSnapshot.readerInfo
            dataFiles = cachedLocalSnapshot.dataFiles
            resourceFiles = cachedLocalSnapshot.resourceFiles
            papers = cachedLocalSnapshot.papers
            errorMessage = null
            isLoading = false
            isParsingLocalAnswers = false
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "已恢复上次读取的本地试卷内容")
            return@LaunchedEffect
        }

        if (!accountOnline) {
            isLoading = false
            isParsingLocalAnswers = false
            errorMessage = if (papers.isEmpty()) "已离线" else null
            return@LaunchedEffect
        }

        LocalReadProgressState.reset()
        
        isLoading = true
        errorMessage = null
        LocalReadProgressState.phase = LocalParsePhase.SCANNING
        LocalReadProgressState.message = "正在扫描试卷列表"
        
        addLog(LogLevel.INIT, LogCategory.SYSTEM, "=".repeat(50))
        addLog(LogLevel.INIT, LogCategory.SYSTEM, "🚀 开始初始化 ETS 100 数据加载")
        addLog(LogLevel.INIT, LogCategory.SYSTEM, "当前激活模式: ${currentMode.name} (${currentMode.title})")
        addLog(LogLevel.INIT, LogCategory.SYSTEM, "=".repeat(50))
        
        try {
            addLog(LogLevel.DEBUG, LogCategory.SYSTEM, "检查模式可用性...")
            if (!ETS100FileReader.isModeAvailable(currentMode, context)) {
                addLog(LogLevel.ERROR, LogCategory.SYSTEM, "当前模式不可用: $currentMode")
                errorMessage = "当前模式不可用: $currentMode"
                isLoading = false
                return@LaunchedEffect
            }
            addLog(LogLevel.SUCCESS, LogCategory.SYSTEM, "✓ 模式检查通过")
            
            // 在 IO 线程执行文件扫描，先把试卷列表显示出来
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "开始扫描试卷列表...")
            val summaryStartMs = System.currentTimeMillis()
            val summaryResult = withContext(Dispatchers.IO) {
                try {
                    // 获取阅读器
                    addLog(LogLevel.DEBUG, LogCategory.FILE, "获取文件阅读器...")
                    val reader = ETS100FileReader.getReader(currentMode, context)
                    addLog(LogLevel.SUCCESS, LogCategory.FILE, "✓ 阅读器创建成功: ${reader.javaClass.simpleName}")
                    
                    // 获取路径信息
                    val dataDirPath = ETS100FileReader.Path.getDataDir()
                    val resourceDirPath = ETS100FileReader.Path.getResourceDir()
                    addLog(LogLevel.DEBUG, LogCategory.FILE, "数据目录: $dataDirPath")
                    addLog(LogLevel.DEBUG, LogCategory.FILE, "资源目录: $resourceDirPath")

                    val shouldScanDebugFileLists = currentMode == ActivationMode.DIRECT_READ
                    
                    val dataFilesList = if (shouldScanDebugFileLists) {
                        addLog(LogLevel.INFO, LogCategory.FILE, "扫描 data 目录...")
                        reader.listFiles(dataDirPath)
                            .filter { file -> !file.isDirectory && file.size > ETS100FileReader.Path.MIN_FILE_SIZE }
                            .sortedByDescending { it.lastModified }
                            .also { files ->
                                addLog(LogLevel.SUCCESS, LogCategory.FILE, "✓ 找到 ${files.size} 个数据文件")
                                if (files.isNotEmpty()) {
                                    addLog(LogLevel.DEBUG, LogCategory.FILE, "  最新文件: ${files.first().name} (${formatFileSize(files.first().size)})")
                                }
                            }
                    } else {
                        addLog(LogLevel.INFO, LogCategory.FILE, "Root/Shizuku 列表阶段跳过 data 明细扫描")
                        emptyList()
                    }
                    
                    val resourceFilesList = if (shouldScanDebugFileLists) {
                        addLog(LogLevel.INFO, LogCategory.FILE, "扫描 resource 目录...")
                        reader.listFiles(resourceDirPath).also { files ->
                            addLog(LogLevel.SUCCESS, LogCategory.FILE, "✓ 找到 ${files.size} 个资源文件")
                        }
                    } else {
                        addLog(LogLevel.INFO, LogCategory.FILE, "Root/Shizuku 列表阶段跳过 resource 明细扫描")
                        emptyList()
                    }
                    
                    // 先轻量读取试卷列表，不在这里解析完整答案
                    addLog(LogLevel.INFO, LogCategory.PAPER, "=" .repeat(40))
                    addLog(LogLevel.INFO, LogCategory.PAPER, "📚 开始读取试卷列表...")
                    val paperList = ETS100AnswerReader.readPaperSummaries(context, currentMode)
                    addLog(LogLevel.SUCCESS, LogCategory.PAPER, "✓ 成功扫描到 ${paperList.size} 份试卷")
                    
                    InitResult.Success(
                        readerInfo = reader.toString(),
                        dataFiles = dataFilesList,
                        resourceFiles = resourceFilesList,
                        papers = paperList
                    )
                } catch (e: Exception) {
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ IO 操作失败: ${e.message}")
                    InitResult.Error(e.message ?: "未知错误")
                }
            }
            val summaryCostMs = System.currentTimeMillis() - summaryStartMs
            
            addLog(LogLevel.INFO, LogCategory.SYSTEM, "处理试卷列表扫描结果...")
            when (summaryResult) {
                is InitResult.Success -> {
                    readerInfo = summaryResult.readerInfo
                    dataFiles = summaryResult.dataFiles
                    resourceFiles = summaryResult.resourceFiles
                    papers = summaryResult.papers
                    errorMessage = null
                    isLoading = false
                    LocalReadProgressState.totalCount = summaryResult.papers.size
                    LocalReadProgressState.doneCount = 0
                    LocalReadProgressState.parsedGroupIndexes = emptySet()
                    LocalReadProgressState.message = "列表扫描耗时 ${summaryCostMs}ms，正在后台解析答案"
                    addLog(
                        LogLevel.SUCCESS,
                        LogCategory.SYSTEM,
                        "✓ 已显示 ${summaryResult.papers.size} 份试卷，列表扫描耗时 ${summaryCostMs}ms，开始后台解析答案"
                    )

                    if (summaryResult.papers.isNotEmpty()) {
                        isParsingLocalAnswers = true
                        LocalReadProgressState.phase = LocalParsePhase.PARSING
                        LocalReadProgressState.message = "列表扫描耗时 ${summaryCostMs}ms，正在后台解析答案"
                        LocalReadProgressState.parseJob = AppCoroutineScope.scope.launch {
                            delay(32)
                            val parseStartMs = System.currentTimeMillis()
                            val fullResult = withContext(Dispatchers.IO) {
                                try {
                                    val paperList = ETS100AnswerReader.readPapersParallel(
                                        context = context,
                                        mode = currentMode
                                    ) { groupIndex, parsedGroupPapers ->
                                        withContext(Dispatchers.Main) {
                                            LocalReadProgressState.parsedGroupIndexes =
                                                LocalReadProgressState.parsedGroupIndexes + groupIndex
                                            LocalReadProgressState.doneCount =
                                                LocalReadProgressState.parsedGroupIndexes.size
                                            if (parsedGroupPapers.isNotEmpty()) {
                                                papers = papers.toMutableList().also { currentPapers ->
                                                    parsedGroupPapers.forEachIndexed { offset, parsedPaper ->
                                                        val existingIndex = currentPapers.indexOfFirst {
                                                            it.paperId == parsedPaper.paperId
                                                        }
                                                        if (existingIndex >= 0) {
                                                            currentPapers[existingIndex] = parsedPaper
                                                        } else {
                                                            val insertIndex = (groupIndex + offset)
                                                                .coerceIn(0, currentPapers.size)
                                                            currentPapers.add(insertIndex, parsedPaper)
                                                        }
                                                    }
                                                }
                                                LocalReadProgressState.sectionCount = papers.totalSectionCount()
                                                LocalReadProgressState.questionCount = papers.totalQuestionCount()
                                                addLog(
                                                    LogLevel.SUCCESS,
                                                    LogCategory.PAPER,
                                                    "✓ 已解析第 ${groupIndex + 1} 份试卷 " +
                                                        "(${LocalReadProgressState.doneCount}/${LocalReadProgressState.totalCount})，" +
                                                        "累计 ${LocalReadProgressState.sectionCount} 个分区、${LocalReadProgressState.questionCount} 道题"
                                                )
                                            }
                                        }
                                    }
                                    InitResult.Success(
                                        readerInfo = summaryResult.readerInfo,
                                        dataFiles = summaryResult.dataFiles,
                                        resourceFiles = summaryResult.resourceFiles,
                                        papers = paperList
                                    )
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    InitResult.Error(e.message ?: "未知错误")
                                }
                            }

                            try {
                                when (fullResult) {
                                    is InitResult.Success -> {
                                        if (fullResult.papers.isNotEmpty()) {
                                            papers = fullResult.papers
                                            ReadPageStateStore.saveLocal(
                                                context,
                                                ReadPageStateStore.LocalSnapshot(
                                                    mode = currentMode,
                                                    readerInfo = fullResult.readerInfo,
                                                    dataFiles = fullResult.dataFiles,
                                                    resourceFiles = fullResult.resourceFiles,
                                                    papers = fullResult.papers
                                                )
                                            )

                                            addLog(LogLevel.INIT, LogCategory.PAPER, "📊 试卷统计:")
                                            addLog(LogLevel.INIT, LogCategory.PAPER, "   总试卷数: ${fullResult.papers.size}")
                                            val totalSections = fullResult.papers.sumOf { it.sections.size }
                                            val totalQuestions = fullResult.papers.sumOf { it.sections.sumOf { s -> s.questions.size } }
                                            val answeredQuestions = fullResult.papers.sumOf { it.sections.sumOf { s -> s.questions.count { q -> q.answer.isNotEmpty() } } }
                                            val answeredPercent = if (totalQuestions > 0) {
                                                (answeredQuestions * 100.0 / totalQuestions).toInt()
                                            } else {
                                                0
                                            }
                                            addLog(LogLevel.INIT, LogCategory.SECTION, "   总分区数: $totalSections")
                                            addLog(LogLevel.INIT, LogCategory.QUESTION, "   总题目数: $totalQuestions")
                                            addLog(LogLevel.SUCCESS, LogCategory.ANSWER, "   已答题目: $answeredQuestions ($answeredPercent%)")
                                            val parseCostMs = System.currentTimeMillis() - parseStartMs
                                            addLog(LogLevel.SUCCESS, LogCategory.SYSTEM, "✓ 答案后台解析完成，耗时 ${parseCostMs}ms")
                                            LocalReadProgressState.doneCount = LocalReadProgressState.totalCount
                                            LocalReadProgressState.sectionCount = totalSections
                                            LocalReadProgressState.questionCount = totalQuestions
                                            LocalReadProgressState.phase = LocalParsePhase.COMPLETED
                                            LocalReadProgressState.message = "共 ${fullResult.papers.size} 份试卷、$totalSections 个分区、$totalQuestions 道题"
                                        } else {
                                            addLog(LogLevel.WARN, LogCategory.SYSTEM, "后台解析未返回完整试卷，保留当前试卷列表")
                                            LocalReadProgressState.phase = LocalParsePhase.FAILED
                                            LocalReadProgressState.message = "后台解析未返回完整试卷"
                                        }
                                    }
                                    is InitResult.Error -> {
                                        addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 后台解析答案失败: ${fullResult.message}")
                                        LocalReadProgressState.phase = LocalParsePhase.FAILED
                                        LocalReadProgressState.message = "后台解析答案失败: ${fullResult.message}"
                                    }
                                }
                            } finally {
                                isParsingLocalAnswers = false
                                LocalReadProgressState.parseJob = null
                            }
                        }
                    } else {
                        addLog(LogLevel.WARN, LogCategory.SYSTEM, "未扫描到试卷，跳过后台答案解析")
                        LocalReadProgressState.phase = LocalParsePhase.COMPLETED
                        LocalReadProgressState.message = "未扫描到试卷"
                    }
                }
                is InitResult.Error -> {
                    addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 加载失败: ${summaryResult.message}")
                    errorMessage = "加载失败: ${summaryResult.message}"
                    LocalReadProgressState.phase = LocalParsePhase.FAILED
                    LocalReadProgressState.message = summaryResult.message
                }
            }
        } catch (e: Exception) {
            addLog(LogLevel.ERROR, LogCategory.SYSTEM, "✗ 初始化异常: ${e.message}")
            errorMessage = "初始化异常: ${e.message}"
            LocalReadProgressState.phase = LocalParsePhase.FAILED
            LocalReadProgressState.message = e.message ?: "未知错误"
        } finally {
            isLoading = false
            addLog(LogLevel.INIT, LogCategory.SYSTEM, "初始化流程结束喵~")
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()

    val onRefresh: () -> Unit = {
        if (readAccessChecking || readAccessIssue != null) {
            onNavigateToActivation()
        } else if (!accountOnline) {
            Toast.makeText(context, "已离线", Toast.LENGTH_SHORT).show()
        } else if (currentMode == ActivationMode.CLOUD) {
            if (homeworkListsByStatus.isNotEmpty()) {
                scope.launch {
                    isRefreshing = true
                    try {
                        loadCloudHomeworkList()
                    } finally {
                        isRefreshing = false
                    }
                }
            } else {
                showCloudReadConfirmDialog = true
            }
        } else {
            scope.launch {
                isRefreshing = true
                try {
                    ETS100AnswerReader.clearResourceScanCache(currentMode)
                    ReadPageStateStore.clearLocal(context)
                    reloadTrigger++
                    delay(400)
                } finally {
                    isRefreshing = false
                }
            }
        }
    }

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal
        ),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("读取答案") },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showReadActionsMenu = true },
                            enabled = accountOnline,
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多操作")
                        }
                        DropdownMenu(
                            expanded = showReadActionsMenu,
                            onDismissRequest = { showReadActionsMenu = false },
                        ) {
                            if (currentMode in setOf(
                                    ActivationMode.ROOT,
                                    ActivationMode.SHIZUKU,
                                    ActivationMode.DIRECT_READ,
                                )
                            ) {
                                DropdownMenuItem(
                                    text = { Text("导出本地源文件") },
                                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                    onClick = {
                                        showReadActionsMenu = false
                                        showLocalDirectoryExport = true
                                    },
                                )
                            } else if (currentMode == ActivationMode.CLOUD) {
                                DropdownMenuItem(
                                    text = { Text("导出云端缓存文件") },
                                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                    onClick = {
                                        showReadActionsMenu = false
                                        showCloudDirectoryExport = true
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (currentMode == ActivationMode.CLOUD) "清除云端缓存" else "清除本地缓存") },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                onClick = {
                                    showReadActionsMenu = false
                                    showDeleteConfirmDialog = true
                                },
                            )
                        }
                    }
                },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            SmallExtendedFloatingActionButton(
                expanded = readFabExpanded,
                onClick = {
                    if (readAccessChecking || readAccessIssue != null) {
                        onNavigateToActivation()
                        return@SmallExtendedFloatingActionButton
                    }
                    if (!accountOnline) {
                        Toast.makeText(context, "已离线", Toast.LENGTH_SHORT).show()
                        return@SmallExtendedFloatingActionButton
                    }
                    if (currentMode == ActivationMode.CLOUD) {
                        showCloudReadConfirmDialog = true
                    } else {
                        ETS100AnswerReader.clearResourceScanCache(currentMode)
                        ReadPageStateStore.clearLocal(context)
                        reloadTrigger++
                    }
                },
                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                text = { Text(if (currentMode == ActivationMode.CLOUD) "读取作业" else "重新读取") },
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.LoadingIndicator(
                    modifier = Modifier
                        .padding(top = paddingValues.calculateTopPadding())
                        .align(Alignment.TopCenter),
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
            when {
                // 1. 读取前置条件不满足时直接显示可操作提示，不进入底层读取流程。
                readAccessChecking || readAccessIssue != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                if (readAccessChecking) "正在检查读取条件" else "暂时无法读取",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                readAccessIssue ?: "正在确认账号、权限和运行模式...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            if (!readAccessChecking) {
                                Spacer(Modifier.height(20.dp))
                                Button(onClick = onNavigateToActivation) {
                                    Text("前往激活配置")
                                }
                            }
                        }
                    }
                }

                // 2. 加载中（本地模式 isLoading 或 云端模式 isLoadingCloudHomework）喵~
                isLoading || (currentMode == ActivationMode.CLOUD && isLoadingCloudHomework) -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            val loadingTitle = if (currentMode == ActivationMode.CLOUD) {
                                "正在加载云端作业..."
                            } else {
                                when (LocalReadProgressState.phase) {
                                    LocalParsePhase.SCANNING -> "正在扫描试卷列表..."
                                    LocalParsePhase.PARSING -> "正在解析答案..."
                                    else -> "正在加载试卷..."
                                }
                            }
                            Text(
                                loadingTitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val statsText = if (currentMode == ActivationMode.CLOUD) {
                                "已下载 ${downloadedCloudPapers.size} 份作业，累计 $downloadedCloudQuestionCount 道题"
                            } else {
                                "已发现 ${papers.size} 份试卷，${loadedLocalSectionCount} 个分区，${loadedLocalQuestionCount} 道题"
                            }
                            Text(
                                statsText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 2. 错误状态（本地 errorMessage 或 云端 cloudHomeworkError）喵~
                errorMessage != null || cloudHomeworkError != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                errorMessage ?: cloudHomeworkError ?: "未知错误",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    if (shouldOpenChangyanLoginDirectly) {
                                        context.startActivity(ChangyanWebLoginActivity.createIntent(context))
                                    } else {
                                        onNavigateToActivation()
                                    }
                                }
                            ) {
                                Text(if (shouldOpenChangyanLoginDirectly) "前往登录" else "前往激活")
                            }
                        }
                    }
                }

                // 3. 云端模式：作业列表非空，显示作业列表或答案详情喵~
                currentMode == ActivationMode.CLOUD -> {
                    // 宝贝用 Crossfade 在作业列表和答案详情之间切换喵~
                    Crossfade(
                        targetState = showPaperDetail && selectedPaper != null,
                        animationSpec = tween(300),
                        label = "cloudPaperDetailCrossfade"
                    ) { showDetail ->
                        if (showDetail && selectedPaper != null) {
                            val paper = selectedPaper!!
                            PaperDetailScreen(
                                paper = paper,
                                onBack = {
                                    showPaperDetail = false
                                    selectedPaper = null
                                },
                                categoryColors = categoryColors,
                                compactAnswerDisplay = compactAnswerDisplay,
                                onCopyText = {
                                    copyPaperText(paper)
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = paperListState,
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(13.dp)
                            ) {
                                item {
                                    CloudModeInfoCard(
                                        selectedStatus = selectedCloudHomeworkStatus,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    CloudHomeworkCategorySelector(
                                        selectedCategory = selectedCloudCategory,
                                        homeworkCount = homeworkList.size,
                                        onCategorySelect = { selectCloudCategory(it) }
                                    )
                                }
                                if (isCurrentCategoryLoaded && homeworkList.isEmpty()) {
                                    item {
                                        EmptyCloudHomeworkCard(
                                            label = selectedCloudHomeworkLabel,
                                            hasLoaded = true,
                                            isVocabPk = selectedCloudCategory == CloudHomeworkState.CATEGORY_VOCAB_PK
                                        )
                                    }
                                } else if (!isCurrentCategoryLoaded) {
                                    item {
                                        EmptyCloudHomeworkCard(
                                            label = selectedCloudHomeworkLabel,
                                            hasLoaded = false,
                                            isVocabPk = selectedCloudCategory == CloudHomeworkState.CATEGORY_VOCAB_PK
                                        )
                                    }
                                }
                                itemsIndexed(
                                    items = homeworkList,
                                    key = { index, homeworkInfo ->
                                        "${selectedCloudCategory}:${cloudHomeworkIdentity(homeworkInfo)}:$index"
                                    },
                                ) { paperIndex, homeworkInfo ->
                                    val targetIdentity = cloudHomeworkIdentity(homeworkInfo)
                                    val currentKey = cloudHomeworkCacheKey(CloudHomeworkState.STATUS_CURRENT, homeworkInfo)
                                    val historyKey = cloudHomeworkCacheKey(CloudHomeworkState.STATUS_HISTORY, homeworkInfo)
                                    val homeworkKey = if (downloadedHomeworkNames.contains(historyKey)) historyKey else currentKey

                                    val downloadedPaper = downloadedPapers[currentKey]?.firstOrNull { it.sections.isNotEmpty() }
                                        ?: downloadedPapers[historyKey]?.firstOrNull { it.sections.isNotEmpty() }
                                        ?: downloadedPapers.entries.firstOrNull { (k, list) ->
                                            (k.endsWith(":$targetIdentity") ||
                                             list.firstOrNull()?.source.let { it is PaperSource.Cloud && it.homeworkIdentity == targetIdentity }) &&
                                            list.firstOrNull()?.sections?.isNotEmpty() == true
                                        }?.value?.firstOrNull()

                                    val isDownloaded = downloadedPaper != null && downloadedPaper.sections.isNotEmpty()
                                    val isDownloading = cloudDownloadingHomeworks.contains(currentKey) ||
                                        cloudDownloadingHomeworks.contains(historyKey) ||
                                        cloudDownloadingHomeworks.any { it.endsWith(":$targetIdentity") }
                                    val isFailed = !isDownloaded && (failedCloudHomeworks.contains(currentKey) ||
                                        failedCloudHomeworks.contains(historyKey) ||
                                        failedCloudHomeworks.any { it.endsWith(":$targetIdentity") })
                                    val downloadProgress = cloudDownloadProgress[currentKey]
                                        ?: cloudDownloadProgress[historyKey]
                                        ?: cloudDownloadProgress.entries.firstOrNull { it.key.endsWith(":$targetIdentity") }?.value
                                    val displayPaper = downloadedPaper ?: createCloudHomeworkPlaceholder(homeworkInfo)

                                    PaperListItem(
                                        paper = displayPaper,
                                        paperIndex = paperIndex,
                                        onClick = {
                                            if (downloadedPaper != null) {
                                                // 宝贝已下载，点击查看详情喵~
                                                openPaperDetail(downloadedPaper)
                                            } else if (!isDownloading) {
                                                // 宝贝未下载，开始下载喵~
                                                val parseStatus = if (selectedCloudCategory == CloudHomeworkState.CATEGORY_HISTORY)
                                                    CloudHomeworkState.STATUS_HISTORY
                                                else
                                                    CloudHomeworkState.STATUS_CURRENT
                                                val targetCacheKey = cloudHomeworkCacheKey(parseStatus, homeworkInfo)

                                                // 立即更新下载中状态，消除等待卡顿感知（0ms 即时反馈）
                                                cloudDownloadingHomeworks = cloudDownloadingHomeworks + targetCacheKey
                                                CloudHomeworkState.cloudDownloadingHomeworks = cloudDownloadingHomeworks
                                                failedCloudHomeworks = failedCloudHomeworks - targetCacheKey
                                                CloudHomeworkState.failedCloudHomeworks = failedCloudHomeworks
                                                updateCloudDownloadProgress(
                                                    targetCacheKey,
                                                    CloudHomeworkState.DownloadProgress(currentFileName = "正在准备解析...")
                                                )

                                                AppCoroutineScope.scope.launch {
                                                    downloadAndParseHomework(homeworkInfo, parseStatus)
                                                }
                                            }
                                        },
                                        categoryColors = categoryColors,
                                        isLoading = isDownloading,
                                        isFailed = isFailed,
                                        isDownloaded = isDownloaded,
                                        downloadProgress = downloadProgress,
                                        isCloudMode = true,
                                        onExportSource = downloadedPaper?.let { downloaded ->
                                            { sourceExportPaper = downloaded }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. 空列表（本地和云端统一处理）喵~
                papers.isEmpty() && homeworkList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Default.FolderOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "未找到任何试卷",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "请确保ETS应用数据已正确配置",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                else -> {
                    // 宝贝使用 Crossfade 实现流畅的切换动画喵~
                    Crossfade(
                        targetState = showPaperDetail && selectedPaper != null,
                        animationSpec = tween(300),
                        label = "paperDetailCrossfade"
                    ) { showDetail ->
                        if (showDetail && selectedPaper != null) {
                            // 宝贝保存到局部变量避免 smart cast 问题喵~
                            val paper = selectedPaper!!
                            // 二级页面：显示试卷详情
                            PaperDetailScreen(
                                paper = paper,
                                onBack = {
                                    showPaperDetail = false
                                    selectedPaper = null
                                },
                                categoryColors = categoryColors,
                                compactAnswerDisplay = compactAnswerDisplay,
                                onCopyText = {
                                    copyPaperText(paper)
                                }
                            )
                        } else {
                            // 一级页面：显示试卷列表
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = paperListState,
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(13.dp)
                            ) {
                                if (LocalReadProgressState.phase != LocalParsePhase.IDLE) {
                                    item {
                                        LocalAnswerParsingCard(
                                            phase = LocalReadProgressState.phase,
                                            doneCount = LocalReadProgressState.doneCount,
                                            totalCount = LocalReadProgressState.totalCount,
                                            sectionCount = LocalReadProgressState.sectionCount,
                                            questionCount = LocalReadProgressState.questionCount,
                                            message = LocalReadProgressState.message
                                        )
                                    }
                                }
                                itemsIndexed(
                                    items = papers,
                                    key = { _, paper -> "${paper.paperId}:${paper.dataFileName}" },
                                ) { paperIndex, paper ->
                                    val isLocalAnswerLoading = paper.isLocalAnswerLoading()
                                    PaperListItem(
                                        paper = paper,
                                        paperIndex = paperIndex,
                                        onClick = {
                                            if (!isLocalAnswerLoading) {
                                                openPaperDetail(paper)
                                            }
                                        },
                                        categoryColors = categoryColors,
                                        isLoading = isLocalAnswerLoading,
                                        isClickEnabled = !isLocalAnswerLoading,
                                        onExportSource = paper.source?.let { { sourceExportPaper = paper } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // 宝贝删除确认对话框喵~
            if (showDeleteConfirmDialog) {
                DeleteConfirmDialog(
                    onDismiss = { showDeleteConfirmDialog = false },
                    onConfirm = {
                        showDeleteConfirmDialog = false
                        // 宝贝检查模式喵~
                        if (currentMode == ActivationMode.CLOUD) {
                            // 云端模式：清除缓存喵~
                            clearCloudHomeworkCache()
                        } else {
                            if (isDeleting) return@DeleteConfirmDialog
                            isDeleting = true
                            scope.launch {
                                try {
                                    // 本地模式：删除 data 和 resource 目录喵~
                                    addLog(LogLevel.WARN, LogCategory.FILE, "🗑️ 开始后台删除 data 和 resource 目录...")

                                    val currentModeValue = currentMode
                                    addLog(LogLevel.INFO, LogCategory.FILE, "   ├─ 删除模式: ${currentModeValue.title}")

                                    val deleteResult = withContext(Dispatchers.IO) {
                                        val dataPath = ETS100FileReader.Path.getDataDir()
                                        val resourcePath = ETS100FileReader.Path.getResourceDir()
                                        val dataDeleted = ETS100FileReader.deleteDirectory(currentModeValue, dataPath, context)
                                        val resourceDeleted = ETS100FileReader.deleteDirectory(currentModeValue, resourcePath, context)
                                        dataDeleted to resourceDeleted
                                    }

                                    if (deleteResult.first) {
                                        addLog(LogLevel.SUCCESS, LogCategory.FILE, "✅ data 目录删除成功")
                                    } else {
                                        addLog(LogLevel.ERROR, LogCategory.FILE, "❌ data 目录删除失败")
                                    }

                                    if (deleteResult.second) {
                                        addLog(LogLevel.SUCCESS, LogCategory.FILE, "✅ resource 目录删除成功")
                                    } else {
                                        addLog(LogLevel.ERROR, LogCategory.FILE, "❌ resource 目录删除失败")
                                    }

                                    ReadPageStateStore.clearLocal(context)
                                    papers = emptyList()
                                    selectedPaper = null
                                    showPaperDetail = false

                                    // 刷新页面重新读取
                                    reloadTrigger++
                                } catch (e: Exception) {
                                    addLog(LogLevel.ERROR, LogCategory.FILE, "❌ 删除失败: ${e.message}")
                                    Toast.makeText(context, "删除失败: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isDeleting = false
                                }
                            }
                        }
                    },
                    isCloudMode = currentMode == ActivationMode.CLOUD
                )
            }

            if (showCloudReadConfirmDialog) {
                CloudReadConfirmDialog(
                    onDismiss = { showCloudReadConfirmDialog = false },
                    onConfirm = {
                        showCloudReadConfirmDialog = false
                        cloudHomeworkError = null
                        scope.launch { loadCloudHomeworkList() }
                    }
                )
            }

            if (isDeleting) {
                DeletingOverlay()
            }
        }
    }
}

    SourceExportDialogHost(
        paper = sourceExportPaper,
        onDismissRequest = { sourceExportPaper = null }
    )
    if (showLocalDirectoryExport) {
        LocalDirectoryExportDialogHost(
            mode = currentMode,
            currentPapers = papers,
            isParsingInProgress = isParsingLocalAnswers,
            onDismissRequest = { showLocalDirectoryExport = false }
        )
    }
    if (showCloudDirectoryExport) {
        CloudDirectoryExportDialogHost(
            currentPapers = downloadedPapers.values.flatten(),
            isDownloadInProgress = cloudDownloadingHomeworks.isNotEmpty(),
            onDismissRequest = { showCloudDirectoryExport = false }
        )
    }
}

@Composable
private fun LocalDirectoryExportDialogHost(
    mode: ActivationMode,
    currentPapers: List<ETS100AnswerReader.Paper>,
    isParsingInProgress: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirmation by remember { mutableStateOf(true) }
    var inspection by remember { mutableStateOf<PaperSourceExporter.LocalDirectoryInspection?>(null) }
    var inspectionError by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    fun shareExport(result: PaperSourceExporter.LocalDirectoryExportResult) {
        Toast.makeText(context, "已保存到 下载/Fe/${result.fileName}", Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享本地题目文件")) }
            .onFailure { Toast.makeText(context, "文件已保存，但没有可用的分享应用", Toast.LENGTH_LONG).show() }
    }

    fun inspectAndExport() {
        if (isPreparing || isExporting) return
        showConfirmation = false
        inspection = null
        inspectionError = null
        isPreparing = true
        val papersSnapshot = currentPapers.toList()
        val parsingInProgressSnapshot = isParsingInProgress
        scope.launch {
            runCatching {
                PaperSourceExporter.inspectLocalDirectoryExport(
                    context = context,
                    mode = mode,
                    currentPapers = papersSnapshot,
                    isParsingInProgress = parsingInProgressSnapshot
                )
            }
                .onSuccess { result ->
                    inspection = result
                    if (result.canExport) {
                        isPreparing = false
                        isExporting = true
                        runCatching { PaperSourceExporter.exportLocalDirectory(context, result) }
                            .onSuccess(::shareExport)
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                Log.e(TAG, "本地目录导出失败", error)
                                Toast.makeText(context, "导出失败：${error.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
                            }
                        isExporting = false
                        onDismissRequest()
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e(TAG, "本地目录预检失败", error)
                    inspectionError = error.message ?: "未知错误"
                }
            isPreparing = false
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) inspectAndExport()
        else Toast.makeText(context, "需要存储权限才能写入下载文件夹", Toast.LENGTH_LONG).show()
    }

    fun beginExport() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            inspectAndExport()
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.Archive, contentDescription = null) },
            title = { Text("导出本地题目文件？") },
            text = {
                Text(
                    "将导出当前本地目录中 data 和 resource 的全部文件，并附上文件对应关系及可解析题目结构。\n\n" +
                        "请确认目录内只保留需要提交给作者处理的内容；不需要的试卷请先删除，之后可重新下载。\n\n" +
                        "导出时间可能较长，请耐心等待。"
                )
            },
            confirmButton = { TextButton(onClick = ::beginExport) { Text("确认导出") } },
            dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } }
        )
    }

    inspection?.takeUnless { it.canExport }?.let { result ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    if (result.isEmpty) "未找到可导出的题目文件" else "当前目录不适合导出"
                )
            },
            text = {
                val text = when {
                    result.isEmpty ->
                        "请先完整下载需要反馈的题目文件。若已下载但仍未检测到文件，可能是该试卷类型暂不支持，或读取出现异常；请截图题目类型后反馈作者。"
                    else -> buildString {
                        append("检测到")
                        if (result.hasTooManyParsedPapers) {
                            append("当前已解析的 ${result.parsedPaperCount} 套试卷")
                        }
                        if (result.hasTooManyParsedPapers && result.exceedsSizeLimit) append("，且")
                        if (result.exceedsSizeLimit) {
                            append(" 本地文件总大小超过 150 MiB")
                        }
                        if (result.isParsingInProgress && result.hasTooManyParsedPapers) {
                            append("；后台解析仍在进行，以上数量不是最终结果")
                        }
                        append("。\n\n仅保留需要答案或无法解析的内容。已可解析且不需要额外处理的试卷请不要导出；文件过多请分批处理。整理方法：全部删除后，手动完整下载需要提交给作者的无法解析试卷。")
                    }
                }
                Text(text)
            },
            confirmButton = { TextButton(onClick = onDismissRequest) { Text("我知道了") } }
        )
    }

    inspectionError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null) },
            title = { Text("无法检查本地目录") },
            text = { Text("预检失败：$error") },
            confirmButton = { TextButton(onClick = onDismissRequest) { Text("关闭") } }
        )
    }

    if (isPreparing || isExporting) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Column {
                        Text(if (isExporting) "正在打包题目文件" else "正在检查本地目录", style = MaterialTheme.typography.titleMedium)
                        Text("请勿关闭页面", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudDirectoryExportDialogHost(
    currentPapers: List<ETS100AnswerReader.Paper>,
    isDownloadInProgress: Boolean,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirmation by remember { mutableStateOf(true) }
    var inspection by remember { mutableStateOf<PaperSourceExporter.CloudDirectoryInspection?>(null) }
    var inspectionError by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    fun shareExport(result: PaperSourceExporter.LocalDirectoryExportResult) {
        Toast.makeText(context, "已保存到 下载/Fe/${result.fileName}", Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享云端缓存文件")) }
            .onFailure { Toast.makeText(context, "文件已保存，但没有可用的分享应用", Toast.LENGTH_LONG).show() }
    }

    fun inspectAndExport() {
        if (isPreparing || isExporting) return
        showConfirmation = false
        inspection = null
        inspectionError = null
        isPreparing = true
        val papersSnapshot = currentPapers.toList()
        scope.launch {
            runCatching {
                PaperSourceExporter.inspectCloudDirectoryExport(
                    context = context,
                    currentPapers = papersSnapshot,
                    isDownloadInProgress = isDownloadInProgress
                )
            }.onSuccess { result ->
                inspection = result
                if (result.canExport) {
                    isPreparing = false
                    isExporting = true
                    runCatching { PaperSourceExporter.exportCloudDirectory(context, result) }
                        .onSuccess(::shareExport)
                        .onFailure { error ->
                            if (error is CancellationException) throw error
                            Log.e(TAG, "云端缓存导出失败", error)
                            Toast.makeText(context, "导出失败：${error.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
                        }
                    isExporting = false
                    onDismissRequest()
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Log.e(TAG, "云端缓存预检失败", error)
                inspectionError = error.message ?: "未知错误"
            }
            isPreparing = false
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) inspectAndExport()
        else Toast.makeText(context, "需要存储权限才能写入下载文件夹", Toast.LENGTH_LONG).show()
    }

    fun beginExport() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            inspectAndExport()
        }
    }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.Archive, contentDescription = null) },
            title = { Text("导出云端缓存文件？") },
            text = {
                Text(
                    "将导出云端缓存目录中的原始 ZIP、解压目录和当前缓存文件，并保留目录结构。\n\n" +
                        "请仅在需要向作者反馈无法解析的云端题目时使用；不需要的缓存请先清除后重新下载目标作业。"
                )
            },
            confirmButton = { TextButton(onClick = ::beginExport) { Text("确认导出") } },
            dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } }
        )
    }

    inspection?.takeUnless { it.canExport }?.let { result ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(if (result.isEmpty) "未找到云端缓存文件" else "当前缓存不适合导出") },
            text = {
                Text(
                    when {
                        result.isEmpty -> "请先完整下载需要反馈的云端作业。"
                        result.isDownloadInProgress -> "云端作业仍在下载，请等待下载结束后再导出。"
                        result.hasTooManyFiles -> "云端缓存文件数量过多，请清除缓存后只下载需要反馈的作业。"
                        else -> buildString {
                            append("检测到")
                            if (result.hasTooManyParsedPapers) append("当前已解析的 ${result.parsedPaperCount} 套试卷")
                            if (result.hasTooManyParsedPapers && result.exceedsSizeLimit) append("，且")
                            if (result.exceedsSizeLimit) append(" 云端缓存总大小超过 150 MiB")
                            append("。\n\n请清除缓存后，只下载需要反馈的无法解析作业，再重新导出。")
                        }
                    }
                )
            },
            confirmButton = { TextButton(onClick = onDismissRequest) { Text("我知道了") } }
        )
    }

    inspectionError?.let { error ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.ErrorOutline, contentDescription = null) },
            title = { Text("无法检查云端缓存") },
            text = { Text("预检失败：$error") },
            confirmButton = { TextButton(onClick = onDismissRequest) { Text("关闭") } }
        )
    }

    if (isPreparing || isExporting) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Column {
                        Text(if (isExporting) "正在打包云端缓存" else "正在检查云端缓存", style = MaterialTheme.typography.titleMedium)
                        Text("请勿关闭页面", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletingOverlay() {
    Dialog(onDismissRequest = {}) {
        FeFilledCard(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "正在删除...",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "请稍等，文件清理在后台执行",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LocalAnswerParsingCard(
    phase: LocalParsePhase,
    doneCount: Int,
    totalCount: Int,
    sectionCount: Int,
    questionCount: Int,
    message: String
) {
    val isFinished = phase == LocalParsePhase.COMPLETED
    val isFailed = phase == LocalParsePhase.FAILED
    val progress = if (totalCount > 0) {
        (doneCount.toFloat() / totalCount).coerceIn(0f, 1f)
    } else {
        null
    }
    val title = when (phase) {
        LocalParsePhase.SCANNING -> "正在扫描试卷列表"
        LocalParsePhase.PARSING -> "正在后台解析答案"
        LocalParsePhase.COMPLETED -> "答案解析完成"
        LocalParsePhase.FAILED -> "答案解析异常"
        LocalParsePhase.IDLE -> "读取准备中"
    }
    val description = when (phase) {
        LocalParsePhase.SCANNING -> message.ifBlank { "正在批量读取资源目录并生成试卷列表" }
        LocalParsePhase.PARSING -> {
            val countText = if (totalCount > 0) "（$doneCount/$totalCount）" else ""
            message.ifBlank { "试卷列表已可查看，答案完成后会自动更新" } + countText
        }
        LocalParsePhase.COMPLETED -> message.ifBlank { "全部试卷已经解析完成" }
        LocalParsePhase.FAILED -> message.ifBlank { "后台解析失败，请重新读取" }
        LocalParsePhase.IDLE -> message
    }
    val statsText = "已解析 $sectionCount 个分区 · $questionCount 道题"
    val iconColor = when {
        isFinished -> MaterialTheme.colorScheme.primary
        isFailed -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val containerColor = if (isFailed) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surfaceBright
    }
    val textColor = if (isFailed) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isFailed) Modifier.border(1.dp, MaterialTheme.colorScheme.error, MaterialTheme.shapes.large)
                else Modifier
            ),
        containerColor = containerColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    isFinished -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    isFailed -> {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = iconColor
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor
                    )
                    if (sectionCount > 0 || questionCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statsText,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor
                        )
                    }
                }
            }
            if (!isFinished && !isFailed) {
                Spacer(modifier = Modifier.height(12.dp))
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun EmptyCloudHomeworkCard(label: String, hasLoaded: Boolean, isVocabPk: Boolean = false) {
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (hasLoaded) {
                    if (isVocabPk) Icons.Default.SportsEsports else Icons.Default.FolderOff
                } else Icons.Default.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (hasLoaded) "未找到$label" else "尚未读取$label",
                    style = MaterialTheme.typography.titleSmall,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (hasLoaded) {
                        if (isVocabPk) "未在当前已同步作业中检测到词汇PK题目" else "该分类暂时没有可用作业"
                    } else "点击右下角“读取作业”获取列表",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CloudHomeworkCategorySelector(
    selectedCategory: String,
    homeworkCount: Int,
    onCategorySelect: (String) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (menuExpanded) 180f else 0f,
        label = "cloudCategoryChevron",
    )

    val categoryTitle = when (selectedCategory) {
        CloudHomeworkState.CATEGORY_HISTORY -> "历史作业"
        CloudHomeworkState.CATEGORY_VOCAB_PK -> "词汇PK"
        else -> "当前作业"
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { menuExpanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = categoryTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = homeworkCount.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (menuExpanded) "收起作业分类" else "选择作业分类",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(chevronRotation),
                )
            }
        }

        // 使用标准锚定菜单，避免按触点偏移后出现贴边、越界或圆角不协调。
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            offset = DpOffset(x = 0.dp, y = 4.dp),
            modifier = Modifier.width(200.dp),
            shape = MaterialTheme.shapes.large,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
        ) {
            listOf(
                "当前作业" to CloudHomeworkState.CATEGORY_CURRENT,
                "历史作业" to CloudHomeworkState.CATEGORY_HISTORY,
                "词汇PK" to CloudHomeworkState.CATEGORY_VOCAB_PK,
            ).forEach { (label, category) ->
                DropdownMenuItem(
                    text = { Text(label, style = MaterialTheme.typography.labelLarge) },
                    trailingIcon = if (selectedCategory == category) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else null,
                    onClick = {
                        menuExpanded = false
                        onCategorySelect(category)
                    },
                )
            }
        }
    }
}

@Composable
private fun PaperCard(
    paper: ETS100AnswerReader.Paper,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    expandedSections: Set<Pair<Int, Int>>,
    onSectionToggle: (Int) -> Unit,
    selectedSectionIndex: Int,
    selectedQuestionIndex: Int,
    onQuestionSelect: (Int, Int) -> Unit,
    categoryColors: Map<String, Color>,
    searchQuery: String,
    showOnlyAnswered: Boolean
) {
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceBright
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            paper.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "${paper.sections.size} 个分区 · ${paper.sections.sumOf { it.questions.size }} 道题目",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Badge(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text("${paper.sections.size}")
                }
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                FeThinDivider()
                Spacer(modifier = Modifier.height(12.dp))
                
                paper.sections.forEachIndexed { sectionIndex, section ->
                    val sectionKey = Pair(paper.sections.indexOf(section), sectionIndex)
                    val isSectionExpanded = sectionKey in expandedSections
                    
                    SectionItem(
                        section = section,
                        sectionIndex = sectionIndex,
                        isExpanded = isSectionExpanded,
                        onToggleExpand = { onSectionToggle(sectionIndex) },
                        selectedQuestionIndex = if (selectedSectionIndex == sectionIndex) selectedQuestionIndex else -1,
                        onQuestionSelect = { onQuestionSelect(sectionIndex, it) },
                        categoryColor = categoryColors[section.category] ?: MaterialTheme.colorScheme.primary,
                        searchQuery = searchQuery,
                        showOnlyAnswered = showOnlyAnswered
                    )
                    
                    if (sectionIndex < paper.sections.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * 试卷详情二级页面
 * 宝贝这个页面显示一个试卷的所有题目喵~
 * 已移除搜索功能，显示所有题目喵~
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperDetailScreen(
    paper: ETS100AnswerReader.Paper,
    onBack: () -> Unit,
    categoryColors: Map<String, Color>,
    compactAnswerDisplay: Boolean = SettingsManager.getCompactAnswerDisplay(),
    onCopyText: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var sourceExportPaper by remember { mutableStateOf<ETS100AnswerReader.Paper?>(null) }
    val context = LocalContext.current
    val defaultPrimaryColor = MaterialTheme.colorScheme.primary
    val categoryPalette = answerCategoryPalette()
    val fallbackCategoryStyle = fallbackAnswerCategoryStyle(defaultPrimaryColor)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ExpressiveScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
        ),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = paper.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                subtitle = {
                    Text("${paper.sections.sumOf { it.questions.size }} 道题目")
                },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
            actions = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "\u66f4\u591a\u64cd\u4f5c"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("\u590d\u5236\u7b54\u6848") },
                            onClick = {
                                showMenu = false
                                onCopyText()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("\u5bfc\u51fa\u56fe\u7247") },
                            onClick = {
                                showMenu = false
                                exportPaperAsImage(context, paper)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Image, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (paper.source != null) "源文件导出" else "源文件不可用") },
                            onClick = {
                                showMenu = false
                                if (paper.source != null) {
                                    sourceExportPaper = paper
                                } else {
                                    Toast.makeText(context, "来源信息已失效，请重新读取该试卷", Toast.LENGTH_LONG).show()
                                }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Archive, contentDescription = null)
                            }
                        )
                    }
                }
            },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            item(key = "paper_metadata") {
                PaperMetadataCard(paper)
            }

            if (paper.sections.none { it.questions.isNotEmpty() }) {
                item(key = "paper_empty") {
                    TonalCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("暂无可显示的答案", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "该试卷没有解析出有效题目，请尝试重新读取。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            paper.sections.forEachIndexed { sectionIndex, section ->
                val categoryStyle = categoryPalette[section.category]
                    ?: fallbackCategoryStyle.copy(accent = categoryColors[section.category] ?: defaultPrimaryColor)

                if (section.questions.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp, 20.dp)
                                    .background(categoryStyle.accent, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = categoryStyle.accent,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = categoryStyle.container.copy(alpha = 0.76f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "${section.displayCategoryLabel()} · ${section.questions.size} 题",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryStyle.onContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 136.dp).padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    val groupedQuestions = section.questions.groupBy { it.originalText }

                    groupedQuestions.entries.forEachIndexed { groupIndex, (_, questionsInGroup) ->
                        item(key = "section:$sectionIndex:group:$groupIndex") {
                            MergedQuestionBlock(
                                groupKey = "${paper.paperId}:$sectionIndex:$groupIndex",
                                sectionTitle = section.title,
                                sectionCategory = section.category,
                                questions = questionsInGroup,
                                categoryStyle = categoryStyle,
                                compactAnswerDisplay = compactAnswerDisplay
                            )
                        }
                    }
                }
            }
        }
    }

    SourceExportDialogHost(
        paper = sourceExportPaper,
        onDismissRequest = { sourceExportPaper = null }
    )
}

@Composable
private fun PaperMetadataCard(paper: ETS100AnswerReader.Paper) {
    val sourceLabel = when (paper.source) {
        is PaperSource.Cloud -> "云端作业"
        is PaperSource.Local -> "本地读取"
        null -> if (paper.regionLabel == "云端") "云端缓存" else "缓存数据"
    }
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DetailInfoItem("来源", sourceLabel)
            DetailInfoItem("地区", paper.regionLabel.takeUnless { it == "未知" }.orEmpty().ifBlank { "未标注" })
            DetailInfoItem("内容", "${paper.sections.size} 个分区 · ${paper.sections.sumOf { it.questions.size }} 道题目")
            if (paper.fileSize > 0L) {
                DetailInfoItem("数据大小", formatFileSize(paper.fileSize))
            }
        }
    }
}

@Composable
private fun DetailInfoItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun ETS100AnswerReader.Section.displayCategoryLabel(): String = when (category) {
    "read_chapter" -> "模仿朗读"
    "simple_expression_ufi" -> typeName.ifBlank { "听说信息" }
    "simple_expression_ufk" -> typeName.ifBlank { "问答信息" }
    "simple_expression_ufj" -> typeName.ifBlank { "询问信息" }
    "topic" -> typeName.ifBlank { "信息转述" }
    ETS100AnswerReader.StructureType.COLLECTOR_READ -> typeName.ifBlank { "模仿朗读" }
    ETS100AnswerReader.StructureType.COLLECTOR_ROLE -> typeName.ifBlank { "问答信息" }
    ETS100AnswerReader.StructureType.COLLECTOR_PICTURE -> typeName.ifBlank { "信息转述" }
    ETS100AnswerReader.StructureType.COLLECTOR_3Q5A -> typeName.ifBlank { "3问5答" }
    ETS100AnswerReader.StructureType.COLLECTOR_CHOOSE -> typeName.ifBlank { "听后选择" }
    ETS100AnswerReader.StructureType.COLLECTOR_FILL -> typeName.ifBlank { "听后记录" }
    ETS100AnswerReader.StructureType.COLLECTOR_DIALOGUE -> typeName.ifBlank { "回答问题" }
    ETS100AnswerReader.StructureType.COLLECTOR_REPEAT_ESSAY -> typeName.ifBlank { "短文复述" }
    ETS100AnswerReader.StructureType.WORD_CHOICE -> typeName.ifBlank { "词汇选择" }
    ETS100AnswerReader.StructureType.WORD_WRITE -> typeName.ifBlank { "单词拼写" }
    ETS100AnswerReader.StructureType.WORD_READ -> typeName.ifBlank { "单词跟读" }
    ETS100AnswerReader.StructureType.WORD_DICTATION -> typeName.ifBlank { "词汇听写" }
    ETS100AnswerReader.StructureType.WORD_TRANSLATE_EN_CN -> typeName.ifBlank { "英译中" }
    ETS100AnswerReader.StructureType.WORD_TRANSLATE_CN_EN -> typeName.ifBlank { "中译英" }
    else -> typeName.ifBlank { category.ifBlank { "其他" } }
}

@Composable
private fun SourceExportDialogHost(
    paper: ETS100AnswerReader.Paper?,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showConfirmation by remember(paper) { mutableStateOf(paper != null) }
    var sourceInspection by remember(paper) { mutableStateOf<PaperSourceExporter.Inspection?>(null) }
    var isExportingSource by remember(paper) { mutableStateOf(false) }

    fun shareExport(result: PaperSourceExporter.ExportResult) {
        Toast.makeText(context, "已保存到 下载/Fe/${result.fileName}", Toast.LENGTH_LONG).show()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, result.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "分享答案源文件")) }
            .onFailure { Toast.makeText(context, "文件已保存，但没有可用的分享应用", Toast.LENGTH_LONG).show() }
    }

    fun exportSource(allowPartial: Boolean) {
        val currentPaper = paper ?: return
        if (isExportingSource) return
        sourceInspection = null
        isExportingSource = true
        scope.launch {
            runCatching { PaperSourceExporter.export(context, currentPaper, allowPartial) }
                .onSuccess(::shareExport)
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Log.e(TAG, "源文件导出失败", error)
                    Toast.makeText(context, "导出失败：${error.message ?: "未知错误"}", Toast.LENGTH_LONG).show()
                }
            isExportingSource = false
            onDismissRequest()
        }
    }

    fun inspectAndExport() {
        val currentPaper = paper ?: return
        val source = currentPaper.source
        if (source == null) {
            Toast.makeText(context, "来源信息已失效，请重新读取该试卷", Toast.LENGTH_LONG).show()
            onDismissRequest()
            return
        }
        isExportingSource = true
        scope.launch {
            runCatching { PaperSourceExporter.inspect(context, source) }
                .onSuccess { inspection ->
                    if (inspection.isComplete) {
                        runCatching { PaperSourceExporter.export(context, currentPaper, false) }
                            .onSuccess(::shareExport)
                            .onFailure { error ->
                                if (error is CancellationException) throw error
                                Toast.makeText(context, "导出失败：${error.message}", Toast.LENGTH_LONG).show()
                            }
                        onDismissRequest()
                    } else {
                        sourceInspection = inspection
                    }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    Toast.makeText(context, "无法核验源文件：${error.message}", Toast.LENGTH_LONG).show()
                    onDismissRequest()
                }
            isExportingSource = false
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) inspectAndExport()
        else Toast.makeText(context, "需要存储权限才能写入下载文件夹", Toast.LENGTH_LONG).show()
    }

    fun beginSourceExport() {
        showConfirmation = false
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            inspectAndExport()
        }
    }

    if (paper != null && showConfirmation) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = { Icon(Icons.Default.Archive, contentDescription = null) },
            title = { Text("导出答案源文件？") },
            text = {
                Text("源文件用于提交给作者适配新题型，不适合作为常规答案分享。\n\n应用将只打包当前整套试卷关联的源文件，并保存到 下载/Fe。")
            },
            confirmButton = { TextButton(onClick = ::beginSourceExport) { Text("确认导出") } },
            dismissButton = { TextButton(onClick = onDismissRequest) { Text("取消") } }
        )
    }

    sourceInspection?.let { inspection ->
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("源文件不完整") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("缺少或冲突的源文件：")
                    (inspection.missing + inspection.conflicts).distinct().forEach { Text("• $it") }
                    Text("仍然导出时，导出说明中会记录以上缺失项。")
                }
            },
            confirmButton = {
                val source = paper?.source
                if (source is PaperSource.Cloud) {
                    TextButton(onClick = {
                        sourceInspection = null
                        isExportingSource = true
                        scope.launch {
                            val updated = runCatching {
                                PaperSourceExporter.downloadMissingCloudFiles(context, source)
                            }.getOrElse {
                                if (it is CancellationException) throw it
                                Toast.makeText(context, "下载缺失文件失败：${it.message}", Toast.LENGTH_LONG).show()
                                PaperSourceExporter.inspect(context, source)
                            }
                            isExportingSource = false
                            if (updated.isComplete) exportSource(false) else sourceInspection = updated
                        }
                    }) { Text("下载缺失文件") }
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = onDismissRequest) { Text("取消") }
                    TextButton(onClick = { exportSource(true) }) { Text("仍然导出") }
                }
            }
        )
    }

    if (isExportingSource) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                    Column {
                        Text("正在准备源文件", style = MaterialTheme.typography.titleMedium)
                        Text("请勿关闭页面", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

private fun ETS100AnswerReader.Paper.firstQuestionPreviewText(): String {
    val firstQuestion = sections
        .asSequence()
        .flatMap { it.questions.asSequence() }
        .firstOrNull { question ->
            question.category != LOCAL_PAPER_LOADING_CATEGORY &&
                question.questionText != "未下载"
        } ?: return ""

    return when {
        firstQuestion.category == "read_chapter" -> {
            firstQuestion.formattedOriginalText.firstSentenceOrLine()
                .ifBlank { firstQuestion.question.firstSentenceOrLine() }
        }
        else -> {
            val choiceOption = (firstQuestion.content as? ETS100AnswerReader.AnswerContent.Choice)
                ?.items
                ?.asSequence()
                ?.flatMap { it.options.asSequence() }
                ?.firstOrNull { it.isNotBlank() }
                ?.trim()
                ?: "${firstQuestion.question}\n${firstQuestion.formattedOriginalText}".firstChoiceOption()
            choiceOption ?: firstQuestion.question.ifBlank { firstQuestion.formattedOriginalText }.firstSentenceOrLine()
        }
    }.normalizePreviewText()
}

private fun String.firstChoiceOption(): String? {
    val match = Regex(
        pattern = """(?is)(?:^|[\s\n])(?:A|①|1)[.．、)]?\s*(.+?)(?=[\s\n](?:B|②|2)[.．、)]?\s*)"""
    ).find(this)

    return match
        ?.groupValues
        ?.getOrNull(1)
        ?.normalizePreviewText()
        ?.takeIf { it.isNotBlank() }
}

private fun String.firstSentenceOrLine(): String {
    val normalized = normalizePreviewText()
    if (normalized.isBlank()) return ""

    val sentenceEnd = listOf("。", "！", "？", ".", "!", "?")
        .map { normalized.indexOf(it) }
        .filter { it >= 0 }
        .minOrNull()

    return if (sentenceEnd != null) {
        normalized.take(sentenceEnd + 1)
    } else {
        normalized.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }
}

private fun String.normalizePreviewText(): String =
    replace(Regex("\\s+"), " ").trim()

private fun ETS100AnswerReader.Paper.localDisplayNumberFromTitle(): Int? =
    Regex("""#(\d+)\s*$""").find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()

/**
 * 试卷列表项组件
 * 宝贝这是一个简单的试卷项目，点击后进入二级页面喵~
 */
@Composable
private fun PaperListItem(
    paper: ETS100AnswerReader.Paper,
    paperIndex: Int,
    onClick: () -> Unit,
    categoryColors: Map<String, Color>,
    isLoading: Boolean = false,
    isFailed: Boolean = false,
    isDownloaded: Boolean = false,  // 宝贝标记是否已下载喵~
    isClickEnabled: Boolean = true,
    downloadProgress: CloudHomeworkState.DownloadProgress? = null,
    isCloudMode: Boolean = false,
    onExportSource: (() -> Unit)? = null
) {
    val localPreviewText = remember(paper) { paper.firstQuestionPreviewText() }
    var showMenu by remember { mutableStateOf(false) }
    val listNumber = if (isCloudMode) {
        paperIndex + 1
    } else {
        paper.localDisplayNumberFromTitle() ?: (paperIndex + 1)
    }
    
    TonalCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isClickEnabled,
                onClick = onClick
            ),
        containerColor = if (isFailed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 宝贝根据状态显示不同的图标喵~
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            when {
                                isLoading -> MaterialTheme.colorScheme.primaryContainer
                                isFailed -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.primaryContainer
                            },
                            MaterialTheme.shapes.medium
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        isFailed -> {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = "加载失败",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        else -> {
                            Text(
                                text = "$listNumber",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 宝贝添加地区标签 Badge 喵~
                        if (paper.regionLabel != "未知") {
                            val regionContainerColor = when (paper.regionLabel) {
                                "初中" -> MaterialTheme.colorScheme.secondaryContainer
                                "高中" -> MaterialTheme.colorScheme.secondaryContainer
                                "词汇PK", "PK/词汇" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceContainer
                            }
                            val regionContentColor = when (paper.regionLabel) {
                                "初中" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "高中" -> MaterialTheme.colorScheme.onSecondaryContainer
                                "词汇PK", "PK/词汇" -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                color = regionContainerColor,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = paper.regionLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = regionContentColor,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = paper.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isDownloaded) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                modifier = Modifier.widthIn(min = 48.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "已下载",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (!isCloudMode && localPreviewText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = localPreviewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    // 宝贝根据状态显示不同的信息喵~
                    when {
                        isLoading -> {
                            val progressText = downloadProgress?.let { progress ->
                                val totalText = if (progress.totalBytes > 0L) {
                                    " / ${formatFileSize(progress.totalBytes)}"
                                } else {
                                    ""
                                }
                                "下载中 ${formatFileSize(progress.downloadedBytes)}$totalText"
                            } ?: "正在加载..."
                            Text(
                                text = progressText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            downloadProgress?.progressFraction?.let { progressFraction ->
                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } ?: LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            downloadProgress?.currentFileName?.takeIf { it.isNotBlank() }?.let { currentFileName ->
                                Text(
                                    text = currentFileName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        isFailed -> {
                            Text(
                                text = if (onExportSource != null) "解析失败，点击重试或导出源文件" else "加载失败，点击重试",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            val countText = "${paper.sections.size} 个分区 · ${paper.sections.sumOf { it.questions.size }} 个内容"
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isCloudMode) {
                                Text(
                                    text = if (isDownloaded) "点击查看答案" else "点击下载并解析",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    text = "点击查看答案",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            if (onExportSource != null) {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "更多操作",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("源文件导出") },
                            onClick = {
                                showMenu = false
                                onExportSource()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Archive, contentDescription = null)
                            }
                        )
                    }
                }
            } else if (!isLoading && !isFailed) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "进入",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 宝贝云端模式信息卡片喵~
 * 显示当前为云端作业模式
 */
@Composable
private fun CloudModeInfoCard(
    selectedStatus: String,
) {
    val isHistory = selectedStatus == CloudHomeworkState.STATUS_HISTORY
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceBright
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FeTonalIconContainer(
                icon = Icons.Default.Cloud,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.54f),
                size = 44.dp,
                iconSize = 24.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isHistory) "历史作业" else "当前作业",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "选择作业下载并解析，已下载项目可直接查看答案",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 宝贝删除确认对话框喵~
 * 用户点击删除按钮后会弹出这个对话框确认
 */
@Composable
private fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isCloudMode: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("确认删除？")
        },
        text = {
            if (isCloudMode) {
                Text("这将清除所有云端缓存的作业数据。\n\n此操作不可撤销！")
            } else {
                Text("这将删除以下目录中的所有文件：\n\n• data/ 目录\n• resource/ 目录\n\n此操作不可撤销！")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CloudReadConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("确认读取云端作业？")
        },
        text = {
            Text(
                "读取云端作业可能会导致 E听说官方客户端退出登录。\n\n" +
                    "请先确认没有正在进行的 E听说练习或考试。建议读取完答案后，再去 E听说做题；否则可能导致 E听说考试或练习连接断开。"
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认读取")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


/**
 * 将试卷格式化为纯文本，用于复制到剪贴板
 */
