package com.shuaiqiu.fuckets100.feature.read

import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveScaffold
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.ChangyanWebLoginActivity
import com.shuaiqiu.fuckets100.CloudHomeworkState
import com.shuaiqiu.fuckets100.ETS100AnswerReader
import com.shuaiqiu.fuckets100.ETS100AuthManager
import com.shuaiqiu.fuckets100.FeAccountFeatures
import com.shuaiqiu.fuckets100.PaperSource
import com.shuaiqiu.fuckets100.SettingsManager
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
    compactAnswerDisplay: Boolean = SettingsManager.getCompactAnswerDisplay(),
    viewModel: ReadViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsState()
    val dataFiles = uiState.dataFiles
    val resourceFiles = uiState.resourceFiles
    val readerInfo = uiState.readerInfo
    val papers = uiState.papers
    val isLoading = uiState.isLoading
    val isParsingLocalAnswers = uiState.isParsingLocalAnswers
    val errorMessage = uiState.errorMessage
    fun openPaperDetail(paper: ETS100AnswerReader.Paper) {
        onOpenPaperDetail(paper)
    }

    fun copyPaperText(paper: ETS100AnswerReader.Paper) {
        val (available, reason) = viewModel.isFeatureAvailable(FeAccountFeatures.ANSWER_SHARE)
        if (!available) {
            android.widget.Toast.makeText(context, reason ?: "答案分享功能当前不可用", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val text = formatPaperAsText(paper)
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText(paper.title, text))
        android.widget.Toast.makeText(context, "已复制答案到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    var selectedPaper by remember { mutableStateOf<ETS100AnswerReader.Paper?>(null) }
    var selectedSectionIndex by remember { mutableIntStateOf(-1) }
    var selectedQuestionIndex by remember { mutableIntStateOf(-1) }

    // 二级页面导航状态 - 宝贝用于切换试卷详情页面喵~
    var showPaperDetail by remember { mutableStateOf(false) }

    // 页面级交互状态
    var sourceExportPaper by remember { mutableStateOf<ETS100AnswerReader.Paper?>(null) }
    var showLocalDirectoryExport by remember { mutableStateOf(false) }
    var showCloudDirectoryExport by remember { mutableStateOf(false) }
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

    val selectedCloudCategory = uiState.selectedCloudCategory
    val selectedCloudHomeworkStatus = uiState.selectedCloudHomeworkStatus
    val homeworkListsByStatus = uiState.homeworkListsByStatus
    val isLoadingCloudHomework = uiState.isLoadingCloudHomework
    val cloudHomeworkError = uiState.cloudHomeworkError
    val downloadedPaperMap = uiState.downloadedPapers
    val downloadedHomeworkNames = uiState.downloadedHomeworkNames
    val cloudDownloadingHomeworks = uiState.cloudDownloadingHomeworks
    val cloudDownloadProgress = uiState.cloudDownloadProgress
    val failedCloudHomeworks = uiState.failedCloudHomeworks
    val isDeleting = uiState.isDeleting

    // 宝贝新增：云端作业占位符列表喵~ 支持当前、历史及词汇PK三类视图
    val homeworkList = remember(selectedCloudCategory, homeworkListsByStatus) {
        ReadDerivedState.homeworkList(selectedCloudCategory, homeworkListsByStatus)
    }
    val selectedCloudHomeworkLabel = remember(selectedCloudCategory) {
        ReadDerivedState.categoryLabel(selectedCloudCategory)
    }
    val isCurrentCategoryLoaded = remember(selectedCloudCategory, homeworkListsByStatus) {
        ReadDerivedState.isCategoryLoaded(selectedCloudCategory, homeworkListsByStatus)
    }
    val cloudHomeworkPlaceholders = remember(homeworkList) {
        homeworkList.map { hw -> createCloudHomeworkPlaceholder(hw) }
    }
    val loadedLocalSectionCount = remember(papers) { papers.totalSectionCount() }
    val loadedLocalQuestionCount = remember(papers) { papers.totalQuestionCount() }
    val downloadedCloudPapers = remember(downloadedPaperMap) {
        ReadDerivedState.downloadedCloudPapers(downloadedPaperMap)
    }
    val downloadedCloudQuestionCount = remember(downloadedCloudPapers) {
        downloadedCloudPapers.totalQuestionCount()
    }
    val shouldOpenChangyanLoginDirectly = remember(currentMode, cloudHomeworkError) {
        currentMode == ActivationMode.CLOUD &&
            cloudHomeworkError != null &&
            ETS100AuthManager.isChangyanWebLogin(context)
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

    // 云端读取副作用（Toast / 打开详情 / 清理选择）由 ViewModel 事件通道驱动。
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReadEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    if (effect.long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
                ).show()

                is ReadEffect.OpenPaperDetail -> openPaperDetail(effect.paper)

                ReadEffect.ClearPaperSelection -> {
                    selectedPaper = null
                    showPaperDetail = false
                }
            }
        }
    }

    LaunchedEffect(currentMode, uiState.reloadTrigger, accountOnline, readAccessChecking, readAccessIssue, viewModel) {
        viewModel.loadLocal(
            mode = currentMode,
            accountOnline = accountOnline,
            readAccessChecking = readAccessChecking,
            readAccessIssue = readAccessIssue,
        )
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
                        viewModel.loadCloudHomeworkList(accountOnline = accountOnline)
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
                    viewModel.reloadLocalWithClearedCache(currentMode)
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
                        viewModel.reloadLocalWithClearedCache(currentMode)
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
                        val loadingPulse = rememberInfiniteTransition(label = "localReadLoadingPulse")
                            .animateFloat(
                                initialValue = 0.72f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(900),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                                label = "localReadLoadingAlpha",
                            )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.animateContentSize(tween(260)),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(44.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = loadingPulse.value),
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val loadingTitle = if (currentMode == ActivationMode.CLOUD) {
                                "正在加载云端作业..."
                            } else {
                                when (uiState.localProgress.phase) {
                                    LocalParsePhase.SCANNING -> "正在扫描试卷列表..."
                                    LocalParsePhase.PARSING -> "正在解析答案..."
                                    else -> "正在加载试卷..."
                                }
                            }
                            AnimatedContent(
                                targetState = loadingTitle,
                                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                                label = "localReadLoadingTitle",
                            ) { title ->
                                Text(
                                    title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val statsText = if (currentMode == ActivationMode.CLOUD) {
                                "已下载 ${downloadedCloudPapers.size} 份作业，累计 $downloadedCloudQuestionCount 道题"
                            } else {
                                "已发现 ${papers.size} 份试卷，${loadedLocalSectionCount} 个分区，${loadedLocalQuestionCount} 道题"
                            }
                            AnimatedContent(
                                targetState = statsText,
                                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                                label = "localReadLoadingStats",
                            ) { stats ->
                                Text(
                                    stats,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
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
                                        onCategorySelect = { viewModel.selectCloudCategory(it) }
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

                                    val downloadedPaper = downloadedPaperMap[currentKey]?.firstOrNull { it.sections.isNotEmpty() }
                                        ?: downloadedPaperMap[historyKey]?.firstOrNull { it.sections.isNotEmpty() }
                                        ?: downloadedPaperMap.entries.firstOrNull { (k, list) ->
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
                                                viewModel.requestHomeworkDownload(
                                                    homeworkInfo = homeworkInfo,
                                                    status = parseStatus,
                                                    accountOnline = accountOnline,
                                                )
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
                                if (uiState.localProgress.phase != LocalParsePhase.IDLE) {
                                    item {
                                        LocalAnswerParsingCard(
                                            phase = uiState.localProgress.phase,
                                            doneCount = uiState.localProgress.doneCount,
                                            totalCount = uiState.localProgress.totalCount,
                                            sectionCount = uiState.localProgress.sectionCount,
                                            questionCount = uiState.localProgress.questionCount,
                                            completedGroupCount = uiState.localProgress.completedGroupCount,
                                            totalGroupCount = uiState.localProgress.totalGroupCount,
                                            activeGroupIndex = uiState.localProgress.activeGroupIndex,
                                            message = uiState.localProgress.message
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
                            viewModel.clearCloudHomeworkCache()
                        } else {
                            // 本地模式：删除 data/resource 目录后重新读取喵~
                            viewModel.deleteLocalReadDirectories(currentMode)
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
                        scope.launch { viewModel.loadCloudHomeworkList(accountOnline = accountOnline) }
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
            currentPapers = downloadedPaperMap.values.flatten(),
            isDownloadInProgress = cloudDownloadingHomeworks.isNotEmpty(),
            onDismissRequest = { showCloudDirectoryExport = false }
        )
    }
}



/**
 * 将试卷格式化为纯文本，用于复制到剪贴板
 */
