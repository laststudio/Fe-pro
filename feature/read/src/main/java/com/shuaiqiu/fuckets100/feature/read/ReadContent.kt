package com.shuaiqiu.fuckets100.feature.read

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.CloudHomeworkState
import com.shuaiqiu.fuckets100.ETS100AnswerReader
import com.shuaiqiu.fuckets100.PaperSource
import com.shuaiqiu.fuckets100.ETS100ApiClient
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.lazy.LazyListState


/** 云端模式：作业列表与答案详情。 */
@Composable
internal fun ReadCloudContent(
    showPaperDetail: Boolean,
    selectedPaper: ETS100AnswerReader.Paper?,
    onCloseDetail: () -> Unit,
    onOpenDetail: (ETS100AnswerReader.Paper) -> Unit,
    onCopyText: (ETS100AnswerReader.Paper) -> Unit,
    onExportSource: (ETS100AnswerReader.Paper) -> Unit,
    onCategorySelect: (String) -> Unit,
    onDownloadHomework: (ETS100ApiClient.HomeworkInfo, String) -> Unit,
    categoryColors: Map<String, Color>,
    compactAnswerDisplay: Boolean,
    paperListState: LazyListState,
    selectedCloudHomeworkStatus: String,
    selectedCloudCategory: String,
    homeworkList: List<ETS100ApiClient.HomeworkInfo>,
    isCurrentCategoryLoaded: Boolean,
    selectedCloudHomeworkLabel: String,
    downloadedHomeworkNames: Set<String>,
    downloadedPaperMap: Map<String, List<ETS100AnswerReader.Paper>>,
    cloudDownloadingHomeworks: Set<String>,
    failedCloudHomeworks: Set<String>,
    cloudDownloadProgress: Map<String, CloudHomeworkState.DownloadProgress>,
    accountOnline: Boolean,
) {
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
                            onBack = onCloseDetail,
                            categoryColors = categoryColors,
                            compactAnswerDisplay = compactAnswerDisplay,
                            onCopyText = {
                                onCopyText(paper)
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
                                    onCategorySelect = { onCategorySelect(it) }
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
                                            onOpenDetail(downloadedPaper)
                                        } else if (!isDownloading) {
                                            // 宝贝未下载，开始下载喵~
                                            val parseStatus = if (selectedCloudCategory == CloudHomeworkState.CATEGORY_HISTORY)
                                                CloudHomeworkState.STATUS_HISTORY
                                            else
                                                CloudHomeworkState.STATUS_CURRENT
                                            val targetCacheKey = cloudHomeworkCacheKey(parseStatus, homeworkInfo)

                                            // 立即更新下载中状态，消除等待卡顿感知（0ms 即时反馈）
                                            onDownloadHomework(homeworkInfo, parseStatus)
                                        }
                                    },
                                    categoryColors = categoryColors,
                                    isLoading = isDownloading,
                                    isFailed = isFailed,
                                    isDownloaded = isDownloaded,
                                    downloadProgress = downloadProgress,
                                    isCloudMode = true,
                                    onExportSource = downloadedPaper?.let { downloaded ->
                                        { onExportSource(downloaded) }
                                    }
                                )
                            }
                        }
                    }
                }
}

/** 本地模式：试卷列表与答案详情。 */
@Composable
internal fun ReadLocalContent(
    showPaperDetail: Boolean,
    selectedPaper: ETS100AnswerReader.Paper?,
    onCloseDetail: () -> Unit,
    onOpenDetail: (ETS100AnswerReader.Paper) -> Unit,
    onCopyText: (ETS100AnswerReader.Paper) -> Unit,
    onExportSource: (ETS100AnswerReader.Paper) -> Unit,
    categoryColors: Map<String, Color>,
    compactAnswerDisplay: Boolean,
    paperListState: LazyListState,
    localProgress: LocalReadProgress,
    papers: List<ETS100AnswerReader.Paper>,
) {
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
                            onBack = onCloseDetail,
                            categoryColors = categoryColors,
                            compactAnswerDisplay = compactAnswerDisplay,
                            onCopyText = {
                                onCopyText(paper)
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
                            if (localProgress.phase != LocalParsePhase.IDLE) {
                                item {
                                    LocalAnswerParsingCard(
                                        phase = localProgress.phase,
                                        doneCount = localProgress.doneCount,
                                        totalCount = localProgress.totalCount,
                                        sectionCount = localProgress.sectionCount,
                                        questionCount = localProgress.questionCount,
                                        completedGroupCount = localProgress.completedGroupCount,
                                        totalGroupCount = localProgress.totalGroupCount,
                                        activeGroupIndex = localProgress.activeGroupIndex,
                                        message = localProgress.message
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
                                            onOpenDetail(paper)
                                        }
                                    },
                                    categoryColors = categoryColors,
                                    isLoading = isLocalAnswerLoading,
                                    isClickEnabled = !isLocalAnswerLoading,
                                    onExportSource = paper.source?.let { { onExportSource(paper) } }
                                )
                            }
                        }
                    }
                }
}

/** 本地与云端都为空时的占位。 */
@Composable
internal fun ReadEmptyContent() {
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
