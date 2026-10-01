package com.shuaiqiu.fuckets100.feature.read

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.CloudHomeworkState
import com.shuaiqiu.fuckets100.ETS100AnswerReader
import com.shuaiqiu.fuckets100.ETS100ApiClient
import com.shuaiqiu.fuckets100.ETS100FileReader
import com.shuaiqiu.fuckets100.FeAccountManager
import com.shuaiqiu.fuckets100.PaperSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@HiltViewModel
class ReadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val accountManager: FeAccountManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReadUiState())
    val uiState: StateFlow<ReadUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ReadEffect>(Channel.BUFFERED)
    val effects: Flow<ReadEffect> = _effects.receiveAsFlow()

    private var activeMode: ActivationMode? = null
    private var cachedLocalSnapshot: ReadPageStateStore.LocalSnapshot? = null
    private var loadJob: Job? = null
    private var parseJob: Job? = null

    init {
        restoreCloudSnapshotIfNeeded()
    }

    fun loadLocal(
        mode: ActivationMode,
        accountOnline: Boolean,
        readAccessChecking: Boolean,
        readAccessIssue: String?,
    ) {
        prepareMode(mode)
        _uiState.update { it.copy(isParsingLocalAnswers = false) }

        if (readAccessChecking || readAccessIssue != null) {
            _uiState.update { it.copy(isLoading = false, isParsingLocalAnswers = false, errorMessage = null) }
            return
        }

        val state = _uiState.value
        if (state.reloadTrigger == 0 &&
            mode != ActivationMode.CLOUD &&
            (state.localProgress.phase == LocalParsePhase.SCANNING ||
                state.localProgress.phase == LocalParsePhase.PARSING)
        ) {
            _uiState.update { it.copy(isLoading = false, isParsingLocalAnswers = true) }
            log("已恢复正在进行的本地答案解析进度")
            return
        }

        if (mode == ActivationMode.CLOUD) {
            log("☁️ 云端模式，等待用户点击读取按钮加载作业列表")
            _uiState.update { it.copy(isLoading = false, isParsingLocalAnswers = false) }
            return
        }

        if (state.reloadTrigger == 0 && cachedLocalSnapshot?.mode == mode) {
            val snapshot = cachedLocalSnapshot ?: return
            _uiState.update {
                it.copy(
                    readerInfo = snapshot.readerInfo,
                    dataFiles = snapshot.dataFiles,
                    resourceFiles = snapshot.resourceFiles,
                    papers = snapshot.papers,
                    errorMessage = null,
                    isLoading = false,
                    isParsingLocalAnswers = false,
                    localProgress = LocalReadProgress(),
                )
            }
            log("已恢复上次读取的本地试卷内容")
            return
        }

        if (!accountOnline) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isParsingLocalAnswers = false,
                    errorMessage = if (it.papers.isEmpty()) "已离线" else null,
                )
            }
            return
        }

        parseJob?.cancel()
        parseJob = null
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                isParsingLocalAnswers = false,
                errorMessage = null,
                localProgress = LocalReadProgress(
                    phase = LocalParsePhase.SCANNING,
                    message = "正在扫描试卷列表",
                ),
            )
        }

        log("=".repeat(50))
        log("🚀 开始初始化 ETS 100 数据加载")
        log("当前激活模式: ${mode.name} (${mode.title})")
        log("=".repeat(50))

        loadJob = viewModelScope.launch {
            try {
                log("检查模式可用性...")
                if (!ETS100FileReader.isModeAvailable(mode, context)) {
                    log("当前模式不可用: $mode", isError = true)
                    _uiState.update {
                        it.copy(
                            errorMessage = "当前模式不可用: $mode",
                            isLoading = false,
                        )
                    }
                    return@launch
                }
                log("✓ 模式检查通过", isSuccess = true)
                log("开始扫描试卷列表...")

                val summaryStartMs = System.currentTimeMillis()
                val summaryResult = ReadLocalLoader.scan(context, mode)
                val summaryCostMs = System.currentTimeMillis() - summaryStartMs

                when (summaryResult) {
                    is LocalReadResult.Success -> {
                        val snapshot = summaryResult.value
                        _uiState.update {
                            it.copy(
                                readerInfo = snapshot.readerInfo,
                                dataFiles = snapshot.dataFiles,
                                resourceFiles = snapshot.resourceFiles,
                                papers = snapshot.papers,
                                errorMessage = null,
                                isLoading = false,
                                localProgress = it.localProgress.copy(
                                    totalCount = snapshot.papers.size,
                                    doneCount = 0,
                                    parsedGroupIndexes = emptySet(),
                                    message = "列表扫描耗时 ${summaryCostMs}ms，正在后台解析答案",
                                ),
                            )
                        }
                        log("✓ 阅读器创建成功: ${snapshot.readerInfo.substringAfterLast('.')}", isSuccess = true)
                        log("✓ 数据文件 ${snapshot.dataFiles.size} 个，资源文件 ${snapshot.resourceFiles.size} 个", isSuccess = true)
                        snapshot.dataFiles.firstOrNull()?.let { file ->
                            log("  最新文件: ${file.name} (${formatFileSize(file.size)})")
                        }
                        log("✓ 成功扫描到 ${snapshot.papers.size} 份试卷", isSuccess = true)
                        log(
                            "✓ 已显示 ${snapshot.papers.size} 份试卷，列表扫描耗时 ${summaryCostMs}ms，开始后台解析答案",
                            isSuccess = true,
                        )

                        if (snapshot.papers.isNotEmpty()) {
                            startLocalParse(mode, snapshot, summaryCostMs)
                        } else {
                            log("未扫描到试卷，跳过后台答案解析", isWarning = true)
                            _uiState.update {
                                it.copy(
                                    localProgress = it.localProgress.copy(
                                        phase = LocalParsePhase.COMPLETED,
                                        message = "未扫描到试卷",
                                    )
                                )
                            }
                        }
                    }

                    is LocalReadResult.Error -> {
                        log("✗ 加载失败: ${summaryResult.message}", isError = true)
                        _uiState.update {
                            it.copy(
                                errorMessage = "加载失败: ${summaryResult.message}",
                                isLoading = false,
                                localProgress = it.localProgress.copy(
                                    phase = LocalParsePhase.FAILED,
                                    message = summaryResult.message,
                                ),
                            )
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("✗ 初始化异常: ${e.message}", isError = true)
                _uiState.update {
                    it.copy(
                        errorMessage = "初始化异常: ${e.message}",
                        isLoading = false,
                        localProgress = it.localProgress.copy(
                            phase = LocalParsePhase.FAILED,
                            message = e.message ?: "未知错误",
                        ),
                    )
                }
            } finally {
                log("初始化流程结束喵~")
            }
        }
    }

    fun isFeatureAvailable(featureKey: String): Pair<Boolean, String?> =
        accountManager.isFeatureAvailable(featureKey)

    suspend fun cloudSession(preferredAccountId: String) =
        accountManager.cloudSession(preferredAccountId)

    fun requestReload() {
        _uiState.update { it.copy(reloadTrigger = it.reloadTrigger + 1, errorMessage = null) }
    }

    fun reloadLocalWithClearedCache(mode: ActivationMode) {
        ETS100AnswerReader.clearResourceScanCache(mode)
        ReadPageStateStore.clearLocal(context)
        cachedLocalSnapshot = null
        requestReload()
    }


    // ==================== 云端模式 ====================

    fun selectCloudCategory(category: String) {
        val state = _uiState.value
        if (category == state.selectedCloudCategory) return

        val targetStatus = cloudStatusForCategory(category)
        CloudHomeworkState.selectedCategory = category
        CloudHomeworkState.selectedStatus = targetStatus
        CloudHomeworkState.error = null
        _uiState.update {
            it.copy(
                selectedCloudCategory = category,
                selectedCloudHomeworkStatus = targetStatus,
                cloudHomeworkError = null,
            )
        }
        // 分类切换只切换当前视图，不自动发起网络读取；由用户点击“读取作业”主动加载。
        _effects.trySend(ReadEffect.ClearPaperSelection)
        saveCloudReadState()
    }

    fun selectCloudHomeworkStatus(status: String) {
        selectCloudCategory(cloudCategoryForStatus(status))
    }

    fun restoreCloudSnapshotIfNeeded() {
        if (cloudSnapshotRestored) return
        cloudSnapshotRestored = true
        val snapshot = ReadPageStateStore.loadCloud(context) ?: return
        if (CloudHomeworkState.homeworkListsByStatus.isNotEmpty() ||
            CloudHomeworkState.downloadedPapers.isNotEmpty()
        ) {
            return
        }
        val (validDownloadedPapers, validHomeworkNames) = ReadDerivedState.sanitizeDownloadedPapers(
            downloadedPapers = snapshot.downloadedPapers,
            downloadedHomeworkNames = snapshot.downloadedHomeworkNames,
        )
        CloudHomeworkState.selectedStatus = snapshot.selectedStatus
        CloudHomeworkState.selectedCategory = snapshot.selectedCategory
        CloudHomeworkState.homeworkListsByStatus = snapshot.homeworkListsByStatus
        CloudHomeworkState.cloudBaseUrl = snapshot.cloudBaseUrl
        CloudHomeworkState.downloadedPapers = validDownloadedPapers
        CloudHomeworkState.downloadedHomeworkNames = validHomeworkNames
        CloudHomeworkState.failedCloudHomeworks = snapshot.failedCloudHomeworks
        _uiState.update {
            it.copy(
                selectedCloudCategory = snapshot.selectedCategory,
                selectedCloudHomeworkStatus = snapshot.selectedStatus,
                homeworkListsByStatus = snapshot.homeworkListsByStatus,
                cloudBaseUrl = snapshot.cloudBaseUrl,
                downloadedPapers = validDownloadedPapers,
                downloadedHomeworkNames = validHomeworkNames,
                failedCloudHomeworks = snapshot.failedCloudHomeworks,
            )
        }
    }

    fun saveCloudReadState() {
        val state = _uiState.value
        ReadPageStateStore.saveCloud(
            context,
            ReadPageStateStore.CloudSnapshot(
                selectedStatus = state.selectedCloudHomeworkStatus,
                homeworkListsByStatus = state.homeworkListsByStatus,
                cloudBaseUrl = state.cloudBaseUrl,
                downloadedPapers = state.downloadedPapers,
                downloadedHomeworkNames = state.downloadedHomeworkNames,
                failedCloudHomeworks = state.failedCloudHomeworks,
                selectedCategory = state.selectedCloudCategory,
            )
        )
    }

    fun updateCloudDownloadProgress(
        cacheKey: String,
        progress: CloudHomeworkState.DownloadProgress?,
    ) {
        _uiState.update { state ->
            val next = if (progress == null) {
                state.cloudDownloadProgress - cacheKey
            } else {
                state.cloudDownloadProgress + (cacheKey to progress)
            }
            CloudHomeworkState.cloudDownloadProgress = next
            state.copy(cloudDownloadProgress = next)
        }
    }

    suspend fun loadCloudHomeworkList(
        status: String = _uiState.value.selectedCloudHomeworkStatus,
        accountOnline: Boolean,
    ) {
        if (_uiState.value.isLoadingCloudHomework) return

        // 先进入加载态，再执行授权校验和账号获取，避免点击后网络操作期间页面像卡死。
        CloudHomeworkState.isLoading = true
        CloudHomeworkState.error = null
        _uiState.update {
            it.copy(
                isLoadingCloudHomework = true,
                cloudHomeworkError = null,
                changyanTokenExpired = false,
                selectedCloudHomeworkStatus = status,
            )
        }
        CloudHomeworkState.selectedStatus = status

        val result = loadCloudHomeworkListResult(
            context = context,
            status = status,
            accountOnline = accountOnline,
            accountManager = accountManager,
        )

        when (result) {
            is CloudHomeworkListResult.Success -> {
                val state = _uiState.value
                val nextLists = state.homeworkListsByStatus + (status to result.homeworks)
                val nextBaseUrl = result.baseUrl.ifBlank { state.cloudBaseUrl }
                CloudHomeworkState.homeworkListsByStatus = nextLists
                CloudHomeworkState.cloudBaseUrl = nextBaseUrl
                CloudHomeworkState.error = null
                CloudHomeworkState.isLoading = false
                _uiState.update {
                    it.copy(
                        homeworkListsByStatus = nextLists,
                        cloudBaseUrl = nextBaseUrl,
                        isLoadingCloudHomework = false,
                        cloudHomeworkError = null,
                        changyanTokenExpired = false,
                    )
                }
                saveCloudReadState()
            }

            is CloudHomeworkListResult.ChangyanExpired -> {
                CloudHomeworkState.error = result.message
                CloudHomeworkState.isLoading = false
                _uiState.update {
                    it.copy(
                        isLoadingCloudHomework = false,
                        cloudHomeworkError = result.message,
                        changyanTokenExpired = true,
                    )
                }
            }

            is CloudHomeworkListResult.Failure -> {
                CloudHomeworkState.error = result.message
                CloudHomeworkState.isLoading = false
                _uiState.update {
                    it.copy(
                        isLoadingCloudHomework = false,
                        cloudHomeworkError = result.message,
                    )
                }
            }
        }
    }

    /** 由界面触发下载；协程生命周期跟随 ViewModel。 */
    fun requestHomeworkDownload(
        homeworkInfo: ETS100ApiClient.HomeworkInfo,
        status: String,
        accountOnline: Boolean,
    ) {
        viewModelScope.launch {
            downloadAndParseHomework(
                homeworkInfo = homeworkInfo,
                status = status,
                accountOnline = accountOnline,
            )
        }
    }

    suspend fun downloadAndParseHomework(
        homeworkInfo: ETS100ApiClient.HomeworkInfo,
        status: String = _uiState.value.selectedCloudHomeworkStatus,
        accountOnline: Boolean,
    ) {
        val state = _uiState.value
        val cacheKey = cloudHomeworkCacheKey(status, homeworkInfo)
        val currentKey = cloudHomeworkCacheKey(CloudHomeworkState.STATUS_CURRENT, homeworkInfo)
        val historyKey = cloudHomeworkCacheKey(CloudHomeworkState.STATUS_HISTORY, homeworkInfo)
        val targetIdentity = cloudHomeworkIdentity(homeworkInfo)

        // 1. 命中已解析缓存则直接打开
        val existingPaper = state.downloadedPapers[cacheKey]?.firstOrNull { it.sections.isNotEmpty() }
            ?: state.downloadedPapers[currentKey]?.firstOrNull { it.sections.isNotEmpty() }
            ?: state.downloadedPapers[historyKey]?.firstOrNull { it.sections.isNotEmpty() }
            ?: state.downloadedPapers.entries.firstOrNull { (k, list) ->
                (k.endsWith(":$targetIdentity") ||
                    list.firstOrNull()?.source.let { it is PaperSource.Cloud && it.homeworkIdentity == targetIdentity }) &&
                    list.firstOrNull()?.sections?.isNotEmpty() == true
            }?.value?.firstOrNull()

        if (existingPaper != null) {
            ReadCloudLog.info("📁 缓存命中，作业已解析喵: ${homeworkInfo.name}")
            val clearedDownloading = state.cloudDownloadingHomeworks - cacheKey - currentKey - historyKey
            val clearedProgress = state.cloudDownloadProgress - cacheKey - currentKey - historyKey
            val nextNames = state.downloadedHomeworkNames + cacheKey
            CloudHomeworkState.cloudDownloadingHomeworks = clearedDownloading
            CloudHomeworkState.cloudDownloadProgress = clearedProgress
            CloudHomeworkState.downloadedHomeworkNames = nextNames
            _uiState.update {
                it.copy(
                    cloudDownloadingHomeworks = clearedDownloading,
                    cloudDownloadProgress = clearedProgress,
                    downloadedHomeworkNames = nextNames,
                )
            }
            _effects.trySend(ReadEffect.OpenPaperDetail(existingPaper))
            return
        }

        if (!accountOnline) {
            val cleared = state.cloudDownloadingHomeworks - cacheKey
            CloudHomeworkState.cloudDownloadingHomeworks = cleared
            _uiState.update { it.copy(cloudDownloadingHomeworks = cleared) }
            _effects.trySend(ReadEffect.ShowToast("已离线"))
            return
        }

        // 进入下载中状态并显示授权校验进度
        val downloading = state.cloudDownloadingHomeworks + cacheKey
        val failed = state.failedCloudHomeworks - cacheKey
        val papersWithoutKey = state.downloadedPapers - cacheKey
        CloudHomeworkState.cloudDownloadingHomeworks = downloading
        CloudHomeworkState.failedCloudHomeworks = failed
        CloudHomeworkState.downloadedPapers = papersWithoutKey
        _uiState.update {
            it.copy(
                cloudDownloadingHomeworks = downloading,
                failedCloudHomeworks = failed,
                downloadedPapers = papersWithoutKey,
            )
        }
        updateCloudDownloadProgress(
            cacheKey,
            CloudHomeworkState.DownloadProgress(currentFileName = "正在校验授权..."),
        )

        val result = downloadAndParseCloudPaper(
            context = context,
            homeworkInfo = homeworkInfo,
            status = status,
            cloudBaseUrl = state.cloudBaseUrl,
            onProgress = { progress ->
                // 下载回调运行在 IO 线程；进度状态保持在主线程更新。
                withContext(Dispatchers.Main) {
                    updateCloudDownloadProgress(cacheKey, progress)
                }
            },
        )

        try {
            when (result) {
                is CloudPaperResult.Success -> {
                    val current = _uiState.value
                    val nextPapers = current.downloadedPapers + (cacheKey to listOf(result.paper))
                    val nextNames = current.downloadedHomeworkNames + cacheKey
                    val nextDownloading = current.cloudDownloadingHomeworks - cacheKey
                    val nextProgress = current.cloudDownloadProgress - cacheKey
                    val nextFailed = current.failedCloudHomeworks - cacheKey
                    CloudHomeworkState.downloadedPapers = nextPapers
                    CloudHomeworkState.downloadedHomeworkNames = nextNames
                    CloudHomeworkState.cloudDownloadingHomeworks = nextDownloading
                    CloudHomeworkState.cloudDownloadProgress = nextProgress
                    CloudHomeworkState.failedCloudHomeworks = nextFailed
                    _uiState.update {
                        it.copy(
                            downloadedPapers = nextPapers,
                            downloadedHomeworkNames = nextNames,
                            cloudDownloadingHomeworks = nextDownloading,
                            cloudDownloadProgress = nextProgress,
                            failedCloudHomeworks = nextFailed,
                        )
                    }
                    saveCloudReadState()
                    // 下载成功后不再切换页面，只标记已下载，停留作业列表喵~
                    _effects.trySend(ReadEffect.ShowToast("下载并解析成功！"))
                }

                is CloudPaperResult.AuthDenied -> {
                    val current = _uiState.value
                    val nextDownloading = current.cloudDownloadingHomeworks - cacheKey
                    val nextProgress = current.cloudDownloadProgress - cacheKey
                    CloudHomeworkState.cloudDownloadingHomeworks = nextDownloading
                    CloudHomeworkState.cloudDownloadProgress = nextProgress
                    _uiState.update {
                        it.copy(
                            cloudDownloadingHomeworks = nextDownloading,
                            cloudDownloadProgress = nextProgress,
                        )
                    }
                    ReadCloudLog.error("✗ ${result.message}")
                    _effects.trySend(ReadEffect.ShowToast(result.message, long = result.longToast))
                }

                is CloudPaperResult.Failure -> {
                    val current = _uiState.value
                    val nextDownloading = current.cloudDownloadingHomeworks - cacheKey
                    val nextProgress = current.cloudDownloadProgress - cacheKey
                    val nextFailed = current.failedCloudHomeworks + cacheKey
                    val nextPapers = result.sourceOnlyPaper?.let { current.downloadedPapers + (cacheKey to listOf(it)) }
                        ?: current.downloadedPapers
                    CloudHomeworkState.cloudDownloadingHomeworks = nextDownloading
                    CloudHomeworkState.cloudDownloadProgress = nextProgress
                    CloudHomeworkState.failedCloudHomeworks = nextFailed
                    CloudHomeworkState.downloadedPapers = nextPapers
                    _uiState.update {
                        it.copy(
                            downloadedPapers = nextPapers,
                            cloudDownloadingHomeworks = nextDownloading,
                            cloudDownloadProgress = nextProgress,
                            failedCloudHomeworks = nextFailed,
                        )
                    }
                    saveCloudReadState()
                    _effects.trySend(ReadEffect.ShowToast("下载或解析失败: ${result.message}", long = true))
                }
            }
        } finally {
            val current = _uiState.value
            if (current.cloudDownloadingHomeworks.contains(cacheKey)) {
                val nextDownloading = current.cloudDownloadingHomeworks - cacheKey
                CloudHomeworkState.cloudDownloadingHomeworks = nextDownloading
                _uiState.update { it.copy(cloudDownloadingHomeworks = nextDownloading) }
            }
        }
    }

    fun clearCloudHomeworkCache() {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            try {
                ReadCloudLog.info("🗑️ 开始后台清除云端缓存")
                withContext(Dispatchers.IO) {
                    val cacheDir = File(context.cacheDir, "cloud_homework")
                    if (cacheDir.exists()) {
                        cacheDir.deleteRecursively()
                    }
                }
                ReadCloudLog.info("🗑️ 已删除云端缓存目录")
                CloudHomeworkState.clear()
                ReadPageStateStore.clearCloud(context)
                _uiState.update {
                    it.copy(
                        downloadedPapers = emptyMap(),
                        downloadedHomeworkNames = emptySet(),
                        cloudDownloadingHomeworks = emptySet(),
                        cloudDownloadProgress = emptyMap(),
                        failedCloudHomeworks = emptySet(),
                        papers = emptyList(),
                        homeworkListsByStatus = emptyMap(),
                        selectedCloudCategory = CloudHomeworkState.CATEGORY_CURRENT,
                        selectedCloudHomeworkStatus = CloudHomeworkState.STATUS_CURRENT,
                    )
                }
                _effects.trySend(ReadEffect.ShowToast("已清除所有云端缓存"))
            } catch (e: Exception) {
                ReadCloudLog.error("✗ 清除云端缓存失败: ${e.message}")
                _effects.trySend(ReadEffect.ShowToast("删除失败: ${e.message}", long = true))
            } finally {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    /** 本地模式：删除 data/resource 目录并重新读取。 */
    fun deleteLocalReadDirectories(mode: ActivationMode) {
        if (_uiState.value.isDeleting) return
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            try {
                ReadCloudLog.warn("🗑️ 开始后台删除 data 和 resource 目录...")
                val (dataDeleted, resourceDeleted) = withContext(Dispatchers.IO) {
                    val dataPath = ETS100FileReader.Path.getDataDir()
                    val resourcePath = ETS100FileReader.Path.getResourceDir()
                    ETS100FileReader.deleteDirectory(mode, dataPath, context) to
                        ETS100FileReader.deleteDirectory(mode, resourcePath, context)
                }
                if (dataDeleted) ReadCloudLog.success("✅ data 目录删除成功") else ReadCloudLog.error("❌ data 目录删除失败")
                if (resourceDeleted) {
                    ReadCloudLog.success("✅ resource 目录删除成功")
                } else {
                    ReadCloudLog.error("❌ resource 目录删除失败")
                }

                loadJob?.cancel()
                parseJob?.cancel()
                loadJob = null
                parseJob = null
                ReadPageStateStore.clearLocal(context)
                cachedLocalSnapshot = null
                _uiState.update {
                    it.copy(
                        readerInfo = "",
                        dataFiles = emptyList(),
                        resourceFiles = emptyList(),
                        papers = emptyList(),
                        isLoading = false,
                        isParsingLocalAnswers = false,
                        errorMessage = null,
                        localProgress = LocalReadProgress(),
                    )
                }
                _effects.trySend(ReadEffect.ClearPaperSelection)
                requestReload()
            } catch (e: Exception) {
                ReadCloudLog.error("❌ 删除失败: ${e.message}")
                _effects.trySend(ReadEffect.ShowToast("删除失败: ${e.message}", long = true))
            } finally {
                _uiState.update { it.copy(isDeleting = false) }
            }
        }
    }

    private var cloudSnapshotRestored = false

    private fun prepareMode(mode: ActivationMode) {
        if (activeMode == mode) return
        loadJob?.cancel()
        parseJob?.cancel()
        loadJob = null
        parseJob = null
        activeMode = mode
        cachedLocalSnapshot = if (mode == ActivationMode.CLOUD) {
            null
        } else {
            ReadPageStateStore.loadLocal(context)?.takeIf { it.mode == mode }
        }
        val snapshot = cachedLocalSnapshot
        _uiState.update {
            if (mode == ActivationMode.CLOUD) {
                it.copy(
                    isLoading = false,
                    isParsingLocalAnswers = false,
                    errorMessage = null,
                    localProgress = LocalReadProgress(),
                )
            } else {
                it.copy(
                    readerInfo = snapshot?.readerInfo.orEmpty(),
                    dataFiles = snapshot?.dataFiles.orEmpty(),
                    resourceFiles = snapshot?.resourceFiles.orEmpty(),
                    papers = snapshot?.papers.orEmpty(),
                    isLoading = snapshot == null,
                    isParsingLocalAnswers = false,
                    errorMessage = null,
                    localProgress = LocalReadProgress(),
                )
            }
        }
    }

    private fun startLocalParse(
        mode: ActivationMode,
        snapshot: LocalReadSnapshot,
        summaryCostMs: Long,
    ) {
        _uiState.update {
            it.copy(
                isParsingLocalAnswers = true,
                localProgress = it.localProgress.copy(
                    phase = LocalParsePhase.PARSING,
                    message = "列表扫描耗时 ${summaryCostMs}ms，正在后台解析答案",
                ),
            )
        }
        parseJob = viewModelScope.launch {
            delay(32)
            val parseStartMs = System.currentTimeMillis()
            val fullResult = ReadLocalLoader.parse(
                context = context,
                mode = mode,
                onGroupStarted = { groupIndex, totalGroups ->
                    _uiState.update {
                        it.copy(
                            localProgress = it.localProgress.copy(
                                totalGroupCount = totalGroups,
                                activeGroupIndex = groupIndex,
                                message = "正在解析第 ${groupIndex + 1}/$totalGroups 组",
                            )
                        )
                    }
                },
                onGroupParsed = { groupIndex, parsedGroupPapers ->
                    val updatedState = _uiState.updateAndGet { current ->
                        val updatedPapers = mergeParsedGroup(
                            currentPapers = current.papers,
                            groupIndex = groupIndex,
                            parsedGroupPapers = parsedGroupPapers,
                        )
                        val parsedIndexes = current.localProgress.parsedGroupIndexes + groupIndex
                        current.copy(
                            papers = updatedPapers,
                            localProgress = current.localProgress.copy(
                                parsedGroupIndexes = parsedIndexes,
                                doneCount = parsedIndexes.size,
                                completedGroupCount = parsedIndexes.size,
                                sectionCount = updatedPapers.totalSectionCount(),
                                questionCount = updatedPapers.totalQuestionCount(),
                            ),
                        )
                    }
                    if (parsedGroupPapers.isNotEmpty()) {
                        log(
                            "✓ 已解析第 ${groupIndex + 1} 份试卷 " +
                                "(${updatedState.localProgress.doneCount}/${updatedState.localProgress.totalCount})，" +
                                "累计 ${updatedState.localProgress.sectionCount} 个分区、" +
                                "${updatedState.localProgress.questionCount} 道题",
                            isSuccess = true,
                        )
                    }
                },
            )

            try {
                when (fullResult) {
                    is LocalReadResult.Success -> {
                        val parsedPapers = fullResult.value
                        if (parsedPapers.isNotEmpty()) {
                            val savedSnapshot = ReadPageStateStore.LocalSnapshot(
                                mode = mode,
                                readerInfo = snapshot.readerInfo,
                                dataFiles = snapshot.dataFiles,
                                resourceFiles = snapshot.resourceFiles,
                                papers = parsedPapers,
                            )
                            ReadPageStateStore.saveLocal(context, savedSnapshot)
                            cachedLocalSnapshot = savedSnapshot
                            val totalSections = parsedPapers.sumOf { it.sections.size }
                            val totalQuestions = parsedPapers.sumOf { it.sections.sumOf { section -> section.questions.size } }
                            val answeredQuestions = parsedPapers.sumOf { paper ->
                                paper.sections.sumOf { section -> section.questions.count { it.answer.isNotEmpty() } }
                            }
                            val answeredPercent = if (totalQuestions > 0) {
                                (answeredQuestions * 100.0 / totalQuestions).toInt()
                            } else {
                                0
                            }
                            val parseCostMs = System.currentTimeMillis() - parseStartMs
                            log("📊 试卷统计:")
                            log("   总试卷数: ${parsedPapers.size}")
                            log("   总分区数: $totalSections")
                            log("   总题目数: $totalQuestions")
                            log("   已答题目: $answeredQuestions ($answeredPercent%)", isSuccess = true)
                            log("✓ 答案后台解析完成，耗时 ${parseCostMs}ms", isSuccess = true)
                            _uiState.update {
                                it.copy(
                                    papers = parsedPapers,
                                    localProgress = it.localProgress.copy(
                                        doneCount = it.localProgress.totalCount,
                                        sectionCount = totalSections,
                                        questionCount = totalQuestions,
                                        phase = LocalParsePhase.COMPLETED,
                                        message = "共 ${parsedPapers.size} 份试卷、$totalSections 个分区、$totalQuestions 道题",
                                    ),
                                )
                            }
                        } else {
                            log("后台解析未返回完整试卷，保留当前试卷列表", isWarning = true)
                            _uiState.update {
                                it.copy(
                                    localProgress = it.localProgress.copy(
                                        phase = LocalParsePhase.FAILED,
                                        message = "后台解析未返回完整试卷",
                                    )
                                )
                            }
                        }
                    }

                    is LocalReadResult.Error -> {
                        log("✗ 后台解析答案失败: ${fullResult.message}", isError = true)
                        _uiState.update {
                            it.copy(
                                localProgress = it.localProgress.copy(
                                    phase = LocalParsePhase.FAILED,
                                    message = "后台解析答案失败: ${fullResult.message}",
                                )
                            )
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isParsingLocalAnswers = false) }
            }
        }
    }


    private fun log(
        message: String,
        isError: Boolean = false,
        isWarning: Boolean = false,
        isSuccess: Boolean = false,
    ) {
        val prefix = when {
            isError -> "[错误] "
            isWarning -> "[警告] "
            isSuccess -> "[成功] "
            else -> "[初始化] "
        }
        val text = "$prefix$message"
        when {
            isError -> Log.e(READ_TAG, text)
            isWarning -> Log.w(READ_TAG, text)
            else -> Log.i(READ_TAG, text)
        }
    }

    private companion object {
        const val READ_TAG = "ReadScreen"
    }
}
