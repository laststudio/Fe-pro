package com.shuaiqiu.fuckets100

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.TonalCard
import com.shuaiqiu.fuckets100.component.material.TopBarBackButton
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors

open class DonateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyPredictiveBackWindowTheme()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeManager.init(this)

        setContent {
            val effectiveDarkMode = if (ThemeManager.getSavedAutoDarkMode()) {
                isSystemInDarkTheme()
            } else {
                ThemeManager.getSavedDarkMode()
            }

            FeThemeWrapper(
                theme = ThemeManager.getSavedTheme(),
                isDarkMode = effectiveDarkMode,
                useDynamicColor = ThemeManager.getSavedDynamicColor()
            ) {
                PredictiveBackContent(onBack = { finish() }) {
                    DonateScreen(onBack = { finish() })
                }
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent {
            return Intent(
                context,
                predictiveBackActivityClass(
                    DonateActivity::class.java,
                    DonateOpaqueActivity::class.java,
                    DonateKernelSuClassicActivity::class.java
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DonateScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ExpressiveScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("捐赠支持") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TonalCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "感谢支持 Fe",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "如果这个项目帮到了你，可以请作者喝杯咖啡。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 600.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        DonateQrCard(
                            title = "微信",
                            imageRes = R.drawable.donate_wechat,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        DonateQrCard(
                            title = "支付宝",
                            imageRes = R.drawable.donate_alipay,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DonateQrCard(
                            title = "微信",
                            imageRes = R.drawable.donate_wechat,
                            modifier = Modifier.weight(1f),
                        )
                        DonateQrCard(
                            title = "支付宝",
                            imageRes = R.drawable.donate_alipay,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonateQrCard(
    title: String,
    imageRes: Int,
    modifier: Modifier = Modifier
) {
    TonalCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "$title 捐赠码",
                modifier = Modifier
                    .sizeIn(maxWidth = 240.dp, maxHeight = 240.dp)
                    .aspectRatio(1f)
                    .heightIn(max = 240.dp),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
