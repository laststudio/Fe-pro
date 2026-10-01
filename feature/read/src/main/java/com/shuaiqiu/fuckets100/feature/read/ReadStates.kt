package com.shuaiqiu.fuckets100.feature.read

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.ActivationMode


@Composable
internal fun ReadAccessGate(
    readAccessChecking: Boolean,
    readAccessIssue: String?,
    onNavigateToActivation: () -> Unit,
) {
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

@Composable
internal fun ReadLoadingState(
    currentMode: ActivationMode,
    localPhase: LocalParsePhase,
    downloadedCloudPaperCount: Int,
    downloadedCloudQuestionCount: Int,
    localPaperCount: Int,
    localSectionCount: Int,
    localQuestionCount: Int,
) {
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
                                when (localPhase) {
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
                                "已下载 ${downloadedCloudPaperCount} 份作业，累计 $downloadedCloudQuestionCount 道题"
                            } else {
                                "已发现 ${localPaperCount} 份试卷，${localSectionCount} 个分区，${localQuestionCount} 道题"
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

@Composable
internal fun ReadErrorState(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
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
                                message,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onAction
                            ) {
                                Text(actionLabel)
                            }
                        }
                    }
}
