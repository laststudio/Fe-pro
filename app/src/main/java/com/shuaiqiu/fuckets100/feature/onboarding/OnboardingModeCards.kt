package com.shuaiqiu.fuckets100.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard

import com.shuaiqiu.fuckets100.ActivationMode
/**
 * 用一句话解释当前模式适合谁、需要什么、会怎么工作。
 * 放在模式卡片和权限卡片之间，让用户先理解选择，再处理授权。
 */
@Composable
internal fun ModeExplanation(mode: ActivationMode) {
    val (title, summary, bullets) = when (mode) {
        ActivationMode.CLOUD -> Triple(
            "推荐给大多数用户",
            "不读取手机本地题库，作业和答案由云端服务处理。",
            listOf(
                "需要：LA Studio 账号 + e听说账号",
                "优点：免 Root、免 Shizuku，配置最少",
                "适合：想快速开始、只关心作业和答案的用户",
            ),
        )
        ActivationMode.SHIZUKU -> Triple(
            "本地读取：Shizuku",
            "直接读取 e听说在手机中缓存的作业数据，不依赖云端绑定。",
            listOf(
                "需要：启动并授权 Shizuku，以及所有文件访问权限",
                "优点：数据留在本机，不需要上传到云端",
                "适合：已经在使用 Shizuku 的高级用户",
            ),
        )
        ActivationMode.ROOT -> Triple(
            "本地读取：Root",
            "使用 Root 权限访问 e听说本地缓存，能力最完整但风险和配置成本更高。",
            listOf(
                "需要：Root 授权，以及所有文件访问权限",
                "优点：本地读取能力最强，不依赖云端绑定",
                "适合：熟悉 Magisk、KernelSU 或 APatch 的用户",
            ),
        )
        ActivationMode.DIRECT_READ, ActivationMode.DEFAULT -> Triple(
            "本地读取：无需 Root",
            "读取手机中已经缓存的作业数据，不使用云端服务。",
            listOf(
                "需要：所有文件访问权限",
                "优点：不需要 Root 或 Shizuku",
                "适合：不想绑定云端，也不想使用提权工具的用户",
            ),
        )
    }

    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            bullets.forEach { bullet ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 1.dp).size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        bullet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** 单选模式卡片 */
@Composable
internal fun ModeSelectionCard(
    title: String,
    subtitle: String,
    badge: String? = null,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    extraContent: (@Composable () -> Unit)? = null,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(220),
        label = "mode_card_color",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220),
        label = "mode_card_border_color",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = tween(220),
        label = "mode_card_border_width",
    )
    val iconContainerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = tween(220),
        label = "mode_icon_container_color",
    )

    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconContainerColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        if (badge != null) {
                            SuggestionChip(
                                onClick = onClick,
                                label = { Text(badge, style = MaterialTheme.typography.labelSmall) },
                                enabled = !selected,
                                modifier = Modifier.height(28.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                )
            }
            if (extraContent != null) {
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(100)) + shrinkVertically(tween(160)),
                ) {
                    extraContent()
                }
            }
        }
    }
}

/** 引导权限项卡片 */
@Composable
internal fun PermissionStatusCard(
    title: String,
    desc: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
) {
    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!isGranted) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onAction,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
