package com.shuaiqiu.fuckets100.feature.read

enum class LocalParsePhase {
    IDLE,
    SCANNING,
    PARSING,
    COMPLETED,
    FAILED,
}

data class LocalReadProgress(
    val phase: LocalParsePhase = LocalParsePhase.IDLE,
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val sectionCount: Int = 0,
    val questionCount: Int = 0,
    val parsedGroupIndexes: Set<Int> = emptySet(),
    val completedGroupCount: Int = 0,
    val totalGroupCount: Int = 0,
    val activeGroupIndex: Int = -1,
    val message: String = "",
)
