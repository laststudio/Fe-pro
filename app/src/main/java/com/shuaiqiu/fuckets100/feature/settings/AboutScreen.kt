package com.shuaiqiu.fuckets100.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveScaffold
import com.shuaiqiu.fuckets100.R
import com.shuaiqiu.fuckets100.BuildConfig
import com.shuaiqiu.fuckets100.core.designsystem.TopBarBackButton
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors

private const val WEBSITE_URL = "https://lastudio.cc"
private const val CLOUD_DRIVE_URL = "https://oplist.lastudio.cc/Fe_android"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AboutScreen(
    onBack: () -> Unit,
    onDonate: () -> Unit,
    onLegal: () -> Unit,
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ExpressiveScaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("关于 Fe") },
                subtitle = { Text("应用信息、资源与法律条款") },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
        ),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.size(4.dp))

            AboutHeroCard()

            AboutSection(title = "应用信息") {
                AboutLinkRow(
                    icon = Icons.Default.Info,
                    title = "版本与软件说明",
                    subtitle = "Fe ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · 专有商业软件",
                    trailingIcon = null,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AboutLinkRow(
                    icon = Icons.Default.Person,
                    title = "开发者",
                    subtitle = "帅丘 · 感谢使用 Fe",
                    trailingIcon = null,
                    leadingContent = {
                        Image(
                            painter = painterResource(R.drawable.ic_author),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                        )
                    },
                )
            }

            AboutSection(title = "资源与支持") {
                AboutLinkRow(
                    icon = Icons.Default.Public,
                    title = "官方网站",
                    subtitle = "lastudio.cc",
                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = { context.openExternal(WEBSITE_URL) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AboutLinkRow(
                    icon = Icons.Default.Cloud,
                    title = "官方网盘",
                    subtitle = "oplist.lastudio.cc/Fe_android",
                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                    onClick = { context.openExternal(CLOUD_DRIVE_URL) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AboutLinkRow(
                    icon = Icons.Default.Favorite,
                    title = "捐赠支持",
                    subtitle = "支持后续开发与维护",
                    onClick = onDonate,
                )
            }

            AboutSection(title = "法律与条款") {
                AboutLinkRow(
                    icon = Icons.Default.Gavel,
                    title = "用户协议、隐私与免责声明",
                    subtitle = "使用前请阅读并遵守",
                    onClick = onLegal,
                )
            }

            Text(
                text = "Copyright © 2026 LastStudio. All rights reserved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun AboutHeroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.fe_logo),
                contentDescription = "应用图标",
                modifier = Modifier.size(88.dp).clip(MaterialTheme.shapes.large),
            )
            Spacer(Modifier.size(16.dp))
            Text(
                text = "Fe",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = "读取、整理与导出 ETS100 作业答案",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = "版本 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun AboutLinkRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailingIcon: ImageVector? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    ListItem(
        modifier = rowModifier,
        leadingContent = leadingContent ?: {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        supportingContent = { Text(subtitle) },
        trailingContent = trailingIcon?.let { resolvedIcon ->
            {
                Icon(
                    imageVector = resolvedIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}

private fun Context.openExternal(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
