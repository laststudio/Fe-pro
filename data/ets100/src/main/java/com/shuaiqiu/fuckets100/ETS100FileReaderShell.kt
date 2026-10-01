package com.shuaiqiu.fuckets100

/**
 * [ETS100FileReader] 使用的 shell 命令构造与输出解析工具。
 *
 * 从 object 中抽出，避免单个文件超过规模预算。
 */
private const val ETS100_LIST_FIELD_SEPARATOR = "\t"

    internal fun shellQuote(value: String): String {
        return "'" + value.replace("'", "'\"'\"'") + "'"
    }

    internal fun buildFastListCommand(path: String): String {
        val quotedPath = shellQuote(path)
        return """
            dir=$quotedPath
            [ -d "${'$'}dir" ] || exit 0
            for f in "${'$'}dir"/* "${'$'}dir"/.[!.]* "${'$'}dir"/..?*; do
              [ -e "${'$'}f" ] || continue
              name=${'$'}{f##*/}
              if [ "${'$'}name" = "." ] || [ "${'$'}name" = ".." ]; then continue; fi
              if [ -d "${'$'}f" ]; then type=d; else type=f; fi
              size=${'$'}(stat -c %s "${'$'}f" 2>/dev/null || echo 0)
              mtime=${'$'}(stat -c %Y "${'$'}f" 2>/dev/null || echo 0)
              mtime_text=${'$'}(stat -c %y "${'$'}f" 2>/dev/null | tr '\t' ' ')
              printf '%s\t%s\t%s\t%s\t%s\n' "${'$'}type" "${'$'}size" "${'$'}mtime" "${'$'}mtime_text" "${'$'}name"
            done
        """.trimIndent()
    }

    internal fun buildFastContentFolderCommand(path: String): String {
        val quotedPath = shellQuote(path)
        return """
            dir=$quotedPath
            [ -d "${'$'}dir" ] || exit 0
            for f in "${'$'}dir"/* "${'$'}dir"/.[!.]* "${'$'}dir"/..?*; do
              [ -d "${'$'}f" ] || continue
              [ -f "${'$'}f/content.json" ] || continue
              name=${'$'}{f##*/}
              if [ "${'$'}name" = "." ] || [ "${'$'}name" = ".." ]; then continue; fi
              size=${'$'}(stat -c %s "${'$'}f/content.json" 2>/dev/null || echo 0)
              mtime=${'$'}(stat -c %Y "${'$'}f" 2>/dev/null || echo 0)
              mtime_text=${'$'}(stat -c %y "${'$'}f" 2>/dev/null | tr '\t' ' ')
              printf '%s\t%s\t%s\t%s\t%s\n' "d" "${'$'}size" "${'$'}mtime" "${'$'}mtime_text" "${'$'}name"
            done
        """.trimIndent()
    }

    internal fun buildDirectorySizeCommand(path: String): String {
        val quotedPath = shellQuote(path)
        return "[ -d $quotedPath ] && du -sk $quotedPath 2>/dev/null | cut -f1 || echo 0"
    }

    internal fun parseDirectorySizeKib(output: String?): Long = output
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.substringBeforeAnyWhitespace()
        ?.toLongOrNull()
        ?.coerceAtLeast(0L)
        ?.coerceAtMost(Long.MAX_VALUE / 1024L)
        ?.times(1024L)
        ?: 0L

    internal fun String.substringBeforeAnyWhitespace(): String =
        indexOfFirst { it.isWhitespace() }
            .takeIf { it >= 0 }
            ?.let { substring(0, it) }
            ?: this

    internal fun parseFastListOutput(output: String): List<ETS100FileReader.FileItem> {
        return output.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split(ETS100_LIST_FIELD_SEPARATOR, limit = 5)
                if (parts.size < 4) return@mapNotNull null
                val name = if (parts.size >= 5) parts[4] else parts[3]
                if (name == "." || name == "..") return@mapNotNull null
                ETS100FileReader.FileItem(
                    name = name,
                    path = "",
                    isDirectory = parts[0] == "d",
                    size = parts[1].toLongOrNull() ?: 0L,
                    lastModified = parseShellStatTimeMillis(
                        secondsText = parts[2],
                        fullTimeText = parts.getOrNull(3).takeIf { parts.size >= 5 }
                    )
                )
            }
            .toList()
    }

    internal fun parseShellStatTimeMillis(secondsText: String, fullTimeText: String?): Long {
        val secondsMillis = (secondsText.toLongOrNull() ?: 0L) * 1000L
        val text = fullTimeText?.trim().orEmpty()
        if (text.isEmpty()) return secondsMillis

        val match = Regex(
            """^(\d{4}-\d{2}-\d{2})[ T](\d{2}:\d{2}:\d{2})(?:\.(\d+))?\s+([+-]\d{4}).*$"""
        ).find(text) ?: return secondsMillis

        return try {
            val baseText = "${match.groupValues[1]} ${match.groupValues[2]} ${match.groupValues[4]}"
            val baseMillis = java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss Z",
                java.util.Locale.US
            ).parse(baseText)?.time ?: secondsMillis
            val fraction = match.groupValues.getOrNull(3).orEmpty()
            val millis = fraction.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            baseMillis + millis
        } catch (e: Exception) {
            secondsMillis
        }
    }
