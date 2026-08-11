package com.shuaiqiu.fuckets100

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.graphics.BitmapFactory
import android.widget.Toast
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shuaiqiu.fuckets100.component.material.ExpressiveScaffold
import com.shuaiqiu.fuckets100.component.material.TonalCard
import com.shuaiqiu.fuckets100.component.material.TopBarBackButton
import com.shuaiqiu.fuckets100.component.material.expressiveTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DonateScreen(onBack: () -> Unit) {
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
    val context = androidx.compose.ui.platform.LocalContext.current
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
                    .fillMaxWidth()
                    .sizeIn(maxWidth = 240.dp, maxHeight = 240.dp)
                    .aspectRatio(1f)
                    .heightIn(max = 240.dp)
                    .pointerInput(imageRes) {
                        detectTapGestures(
                            onLongPress = { saveDonateImage(context, title, imageRes) },
                        )
                    },
                contentScale = ContentScale.Fit,
            )
        }
    }
}

private fun saveDonateImage(context: Context, title: String, imageRes: Int) {
    runCatching {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            (context as? Activity)?.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    4201,
                )
            }
            Toast.makeText(context, "请允许存储权限后再次长按图片", Toast.LENGTH_LONG).show()
            return@runCatching
        }

        val bitmap = BitmapFactory.decodeResource(context.resources, imageRes)
            ?: error("无法读取图片")
        val fileName = "Fe_${title}_捐赠码.png"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Fe")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("无法创建图片文件")
            try {
                resolver.openOutputStream(uri, "w")?.use { output ->
                    check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output))
                } ?: error("无法写入图片")
                resolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null,
                )
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }
        } else {
            val directory = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Fe",
            ).apply { mkdirs() }
            val target = java.io.File(directory, fileName)
            target.outputStream().use { output ->
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output))
            }
            MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf("image/png"), null)
        }
        Toast.makeText(context, "已保存到图片/Fe", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "保存图片失败: ${it.message}", Toast.LENGTH_LONG).show()
    }
}
