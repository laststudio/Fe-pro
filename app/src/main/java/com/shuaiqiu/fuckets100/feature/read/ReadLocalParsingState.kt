package com.shuaiqiu.fuckets100.feature.read

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job

internal enum class LocalParsePhase {
    IDLE,
    SCANNING,
    PARSING,
    COMPLETED,
    FAILED
}

internal object LocalReadProgressState {
    var phase by mutableStateOf(LocalParsePhase.IDLE)
    var doneCount by mutableIntStateOf(0)
    var totalCount by mutableIntStateOf(0)
    var sectionCount by mutableIntStateOf(0)
    var questionCount by mutableIntStateOf(0)
    var parsedGroupIndexes by mutableStateOf(emptySet<Int>())
    var completedGroupCount by mutableIntStateOf(0)
    var totalGroupCount by mutableIntStateOf(0)
    var activeGroupIndex by mutableIntStateOf(-1)
    var message by mutableStateOf("")
    var parseJob: Job? = null

    fun reset() {
        parseJob?.cancel()
        parseJob = null
        phase = LocalParsePhase.IDLE
        doneCount = 0
        totalCount = 0
        sectionCount = 0
        questionCount = 0
        parsedGroupIndexes = emptySet()
        completedGroupCount = 0
        totalGroupCount = 0
        activeGroupIndex = -1
        message = ""
    }
}
