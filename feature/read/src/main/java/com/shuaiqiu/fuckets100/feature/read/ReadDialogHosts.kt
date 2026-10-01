package com.shuaiqiu.fuckets100.feature.read

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.shuaiqiu.fuckets100.ActivationMode
import com.shuaiqiu.fuckets100.ETS100AnswerReader


/** 导出与缓存相关的 Dialog Host 集合。 */
@Composable
internal fun ReadDialogHosts(
    currentMode: ActivationMode,
    papers: List<ETS100AnswerReader.Paper>,
    isParsingLocalAnswers: Boolean,
    downloadedPaperMap: Map<String, List<ETS100AnswerReader.Paper>>,
    cloudDownloadingHomeworks: Set<String>,
    sourceExportPaper: ETS100AnswerReader.Paper?,
    onClearSourceExport: () -> Unit,
    showLocalDirectoryExport: Boolean,
    onDismissLocalDirectoryExport: () -> Unit,
    showCloudDirectoryExport: Boolean,
    onDismissCloudDirectoryExport: () -> Unit,
) {
SourceExportDialogHost(
    paper = sourceExportPaper,
    onDismissRequest = onClearSourceExport
)
if (showLocalDirectoryExport) {
    LocalDirectoryExportDialogHost(
        mode = currentMode,
        currentPapers = papers,
        isParsingInProgress = isParsingLocalAnswers,
        onDismissRequest = onDismissLocalDirectoryExport
    )
}
if (showCloudDirectoryExport) {
    CloudDirectoryExportDialogHost(
        currentPapers = downloadedPaperMap.values.flatten(),
        isDownloadInProgress = cloudDownloadingHomeworks.isNotEmpty(),
        onDismissRequest = onDismissCloudDirectoryExport
    )
}
}
