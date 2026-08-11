package com.shuaiqiu.fuckets100

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

@Composable
fun AnswerScreen(
    paperKey: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val paper = remember(paperKey) { PaperStore.get(paperKey) }

    if (paper == null) {
        LaunchedEffect(paperKey) {
            Toast.makeText(context, "试卷数据已失效，请重新打开", Toast.LENGTH_SHORT).show()
            onBack()
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    PaperDetailScreen(
        paper = paper,
        onBack = onBack,
        categoryColors = answerCategoryColors(),
        onCopyText = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(paper.title, formatPaperAsText(paper)))
            Toast.makeText(context, "已复制答案到剪贴板", Toast.LENGTH_SHORT).show()
        },
    )
}
