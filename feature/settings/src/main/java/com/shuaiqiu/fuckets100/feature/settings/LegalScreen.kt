package com.shuaiqiu.fuckets100.feature.settings

import com.shuaiqiu.fuckets100.feature.shared.LegalContent

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.shuaiqiu.fuckets100.core.designsystem.ExpressiveScaffold
import com.shuaiqiu.fuckets100.core.designsystem.TonalCard
import com.shuaiqiu.fuckets100.core.designsystem.TopBarBackButton
import com.shuaiqiu.fuckets100.core.designsystem.expressiveTopAppBarColors

import com.shuaiqiu.fuckets100.LegalAgreement
import com.shuaiqiu.fuckets100.LegalAgreementSection
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LegalScreen(
    onBack: () -> Unit,
    viewModel: LegalViewModel = hiltViewModel(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val agreement by viewModel.currentAgreement.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("用户协议与免责声明") },
                subtitle = { Text("生效日期：${agreement.effectiveDate} · 版本 ${agreement.version}") },
                navigationIcon = { TopBarBackButton(onClick = onBack) },
                windowInsets = WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            LegalContent(showHeader = true, agreement = agreement)
            Spacer(Modifier.height(28.dp))
        }
    }
}
