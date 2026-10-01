package com.shuaiqiu.fuckets100.feature.activation

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.shuaiqiu.fuckets100.ZWCHelper
@Composable
internal fun FeDirectReadActivationPanel(hasAllBasicPermissions: Boolean) {
    val context = LocalContext.current
    var isDirectReadAvailable by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()

    suspend fun refreshAvailability() {
        isDirectReadAvailable = withContext(Dispatchers.IO) {
            ZWCHelper.isDirectReadAvailable()
        }
    }

    LaunchedEffect(Unit) { refreshAvailability() }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshScope.launch { refreshAvailability() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasAllBasicPermissions) {
        if (isDirectReadAvailable) {
            OutlinedButton(
                onClick = {},
                enabled = false,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(
                    disabledContentColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.fillMaxWidth().height(40.dp),
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("漏洞直读已就绪", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = {
                    if (isTesting) return@Button
                    isTesting = true
                    refreshScope.launch {
                        val available = withContext(Dispatchers.IO) {
                            ZWCHelper.isDirectReadAvailable()
                        }
                        isDirectReadAvailable = available
                        isTesting = false
                        Toast.makeText(
                            context,
                            if (available) "漏洞直读测试通过！" else "暂不支持漏洞直读，请授权全文件访问或使用其他模式",
                            if (available) Toast.LENGTH_SHORT else Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                enabled = !isTesting,
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.BugReport, null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(if (isTesting) "测试中..." else "测试漏洞直读", fontWeight = FontWeight.Bold)
            }
        }
    } else {
        Text(
            text = "请先完成文件访问授权，然后返回此处测试漏洞",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
