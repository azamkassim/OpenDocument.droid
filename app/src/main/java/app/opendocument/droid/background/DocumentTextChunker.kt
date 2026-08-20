package app.opendocument.droid.background

/** Splits rendered document text into bounded chunks without silently dropping the tail. */
object DocumentTextChunker {
    fun chunk(text: String, maxChars: Int, overlapChars: Int = 0): List<String> {
        require(maxChars > 0) { "maxChars must be positive" }
        require(overlapChars >= 0) { "overlapChars must not be negative" }
        require(overlapChars < maxChars) { "overlapChars must be smaller than maxChars" }

        val normalized = text.trim()
        if (normalized.isEmpty()) return emptyList()
        if (normalized.length <= maxChars) return listOf(normalized)

        val chunks = mutableListOf<String>()
        var start = 0

        while (start < normalized.length) {
            val hardEnd = minOf(start + maxChars, normalized.length)
            var end = hardEnd

            if (hardEnd < normalized.length) {
                val paragraphBreak = normalized.lastIndexOf("\n\n", hardEnd - 1)
                val lineBreak = normalized.lastIndexOf('\n', hardEnd - 1)
                val sentenceBreak = normalized.lastIndexOf(". ", hardEnd - 1)
                val candidate = maxOf(paragraphBreak + 2, lineBreak + 1, sentenceBreak + 2)
                if (candidate > start + maxChars / 2) end = candidate
            }

            val chunk = normalized.substring(start, end).trim()
            if (chunk.isNotEmpty()) chunks += chunk
            if (end >= normalized.length) break

            start = maxOf(end - overlapChars, start + 1)
        }

        return chunks
    }
}
