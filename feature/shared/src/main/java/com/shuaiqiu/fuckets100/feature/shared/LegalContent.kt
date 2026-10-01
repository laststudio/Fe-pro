package com.shuaiqiu.fuckets100.feature.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveScaffold
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import com.shuaiqiu.fuckets100.core.designsystem.TopBarBackButton
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors
import com.shuaiqiu.fuckets100.LegalAgreement
import com.shuaiqiu.fuckets100.LegalAgreementSection
import androidx.compose.runtime.Composable

@Composable
fun LegalContent(
    showHeader: Boolean,
    agreement: LegalAgreement,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (showHeader) {
            TonalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = agreement.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "版本: ${agreement.version}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "生效: ${agreement.effectiveDate}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = "适用生态：${agreement.scope}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        agreement.sections.forEach { section ->
            LegalSectionCard(section = section)
        }
    }
}

@Composable
private fun LegalSectionCard(section: LegalAgreementSection) {
    val isWarning = section.warning
    val icon: ImageVector = when {
        isWarning -> Icons.Default.Warning
        section.id.contains("privacy") || section.title.contains("隐私") -> Icons.Default.Lock
        section.id.contains("account") || section.title.contains("账号") -> Icons.Default.Security
        section.id.contains("section-1") || section.title.contains("服务") || section.title.contains("许可") -> Icons.Default.Gavel
        else -> Icons.Default.Info
    }
    val iconTint = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val titleColor = if (isWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    TonalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                }
                val badgeText = section.badge?.takeIf { it.isNotBlank() }
                if (badgeText != null) {
                    Surface(
                        color = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            val contentText = section.content?.takeIf { it.isNotBlank() }
            if (contentText != null) {
                Text(
                    text = contentText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            if (section.items.isNotEmpty()) {
                section.items.forEach { item ->
                    Column(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (item.title.isNotBlank()) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = item.content,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
