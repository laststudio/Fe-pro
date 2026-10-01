package com.shuaiqiu.fuckets100.feature.account

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

import com.shuaiqiu.fuckets100.FeCapSolver
@Composable
internal fun CapVerificationDialog(
    onDismiss: () -> Unit,
    onToken: (String) -> Unit,
) {
    val currentToken by rememberUpdatedState(onToken)
    val solver = remember { FeCapSolver() }
    var solveAttempt by remember { mutableIntStateOf(0) }
    var solveState by remember { mutableStateOf<CapSolveState>(CapSolveState.Solving(0f, "正在连接安全验证服务...")) }

    LaunchedEffect(solveAttempt) {
        solveState = CapSolveState.Solving(0f, "正在获取安全验证挑战...")
        try {
            val result = withContext(Dispatchers.IO) {
                solver.solve { progress ->
                    val stage = when {
                        progress < 0.15f -> "正在获取安全挑战..."
                        progress < 0.90f -> "正在计算工作量证明 (${(progress * 100).toInt()}%)..."
                        else -> "正在校验运行环境与提交凭证..."
                    }
                    solveState = CapSolveState.Solving(progress, stage)
                }
            }
            val token = result.getOrThrow()
            solveState = CapSolveState.Success
            delay(350)
            currentToken(token)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            solveState = CapSolveState.Error(e.message ?: "验证失败，请重试")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = when (solveState) {
                    is CapSolveState.Success -> MaterialTheme.colorScheme.primaryContainer
                    is CapSolveState.Error -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (solveState) {
                            is CapSolveState.Success -> Icons.Default.CheckCircle
                            is CapSolveState.Error -> Icons.Default.ErrorOutline
                            else -> Icons.Default.Security
                        },
                        contentDescription = null,
                        tint = when (solveState) {
                            is CapSolveState.Success -> MaterialTheme.colorScheme.primary
                            is CapSolveState.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        },
        title = {
            Text(
                text = "安全人机验证",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (val state = solveState) {
                    is CapSolveState.Solving -> {
                        val animatedProgress by animateFloatAsState(
                            targetValue = state.progress,
                            animationSpec = tween(durationMillis = 150),
                            label = "cap_progress",
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(92.dp),
                        ) {
                            if (animatedProgress <= 0.01f) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(80.dp),
                                    strokeWidth = 6.dp,
                                )
                            } else {
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(80.dp),
                                    strokeWidth = 6.dp,
                                )
                                Text(
                                    text = "${(animatedProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = state.stage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "纯原生并行算法加速 · 无需加载浏览器组件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    is CapSolveState.Success -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(92.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(72.dp),
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "验证通过",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = "正在完成后续操作...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    is CapSolveState.Error -> {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(92.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(72.dp),
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "验证失败",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (solveState is CapSolveState.Error) {
                Button(
                    onClick = { solveAttempt++ },
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("重试")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private sealed interface CapSolveState {
    data class Solving(val progress: Float, val stage: String) : CapSolveState
    object Success : CapSolveState
    data class Error(val message: String) : CapSolveState
}
