package app.opendocument.droid.intelligence

/**
 * Produces small representative text windows without allocating the whole document again.
 *
 * Fast actions use beginning/middle/end coverage instead of blindly taking only the prefix, which makes large
 * documents feel quick while still sampling information that appears later in the file.
 */
object DocumentTextWindowing {
    fun representativeSample(
        text: String,
        maxChars: Int,
        windows: Int,
    ): String {
        if (text.length <= maxChars) return text
        if (windows <= 1) return text.take(maxChars)

        val windowSize = (maxChars / windows).coerceAtLeast(1)
        val lastStart = (text.length - windowSize).coerceAtLeast(0)
        val starts =
            (0 until windows).map { index ->
                if (windows == 1) 0 else ((lastStart.toLong() * index) / (windows - 1)).toInt()
            }

        val separator = "\n…\n"
        val separatorBudget = separator.length * (starts.size - 1)
        val contentBudget = (maxChars - separatorBudget).coerceAtLeast(starts.size)
        val adjustedWindow = (contentBudget / starts.size).coerceAtLeast(1)

        return starts.joinToString(separator) { start ->
            text.substring(start, (start + adjustedWindow).coerceAtMost(text.length))
        }.take(maxChars)
    }

    fun chunks(
        text: String,
        chunkChars: Int,
        overlapChars: Int,
    ): Sequence<TextChunk> = sequence {
        if (text.isEmpty()) return@sequence

        val step = (chunkChars - overlapChars).coerceAtLeast(1)
        var start = 0
        var index = 0
        while (start < text.length) {
            val end = (start + chunkChars).coerceAtMost(text.length)
            yield(TextChunk(index = index, start = start, endExclusive = end, text = text.substring(start, end)))
            if (end == text.length) break
            start += step
            index += 1
        }
    }
}

data class TextChunk(
    val index: Int,
    val start: Int,
    val endExclusive: Int,
    val text: String,
)
