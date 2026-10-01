package com.shuaiqiu.fuckets100.feature.onboarding

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.feature.shared.EtingshuoBindingCard
import kotlinx.coroutines.launch

import com.shuaiqiu.fuckets100.ChangyanWebLoginActivity
import com.shuaiqiu.fuckets100.FeAccountManager
// ============================================================================
// 云端模式：明确完成 e听说账号绑定
// ============================================================================

@Composable
internal fun CloudBindingStep(
    accountManager: FeAccountManager,
    onFinish: () -> Unit,
) {
    val context = LocalContext.current
    val changyanLoginLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) onFinish()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "绑定 e听说账号",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "云端模式还需要一个 e听说（ETS100）学生账号来读取作业。它和刚才的 LA Studio 账号不是同一个账号。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))

        EtingshuoBindingCard(
            manager = accountManager,
            modifier = Modifier.fillMaxWidth(),
            onChangyanLogin = {
                changyanLoginLauncher.launch(ChangyanWebLoginActivity.createIntent(context))
            },
        )

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text("完成设置，进入 Fe", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        TextButton(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("暂不绑定，先进入应用")
        }
        Text(
            "之后仍可在「设置 > 云端模式」或「个人中心」补充绑定。未绑定时云端作业读取无法使用。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
    }
}
