package app.opendocument.droid.background

/** Splits rendered document text into bounded chunks without silently dropping the tail. */
object DocumentTextChunker {
    data class TextChunk(
        val index: Int,
        val startOffset: Int,
        val endOffsetExclusive: Int,
        val text: String,
    ) {
        val sourceLabel: String
            get() = "Chunk ${index + 1} · chars $startOffset-${endOffsetExclusive - 1}"
    }

    fun chunk(text: String, maxChars: Int, overlapChars: Int = 0): List<String> =
        chunkWithOffsets(text, maxChars, overlapChars).map { it.text }

    fun chunkWithOffsets(text: String, maxChars: Int, overlapChars: Int = 0): List<TextChunk> {
        require(maxChars > 0) { "maxChars must be positive" }
        require(overlapChars >= 0) { "overlapChars must not be negative" }
        require(overlapChars < maxChars) { "overlapChars must be smaller than maxChars" }

        val normalized = text.trim()
        if (normalized.isEmpty()) return emptyList()

        val chunks = mutableListOf<TextChunk>()
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

            val chunkText = normalized.substring(start, end).trim()
            if (chunkText.isNotEmpty()) {
                chunks +=
                    TextChunk(
                        index = chunks.size,
                        startOffset = start,
                        endOffsetExclusive = end,
                        text = chunkText,
                    )
            }
            if (end >= normalized.length) break

            start = maxOf(end - overlapChars, start + 1)
        }

        return chunks
    }
}
