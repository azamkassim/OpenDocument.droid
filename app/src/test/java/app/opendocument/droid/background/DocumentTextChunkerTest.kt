package app.opendocument.droid.background

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentTextChunkerTest {
    @Test
    fun returnsWholeTextWhenItFits() {
        assertEquals(listOf("alpha beta"), DocumentTextChunker.chunk("  alpha beta  ", 20))
    }

    @Test
    fun preservesLongDocumentAcrossChunks() {
        val text = (1..40).joinToString("\n\n") { "Paragraph $it has useful document evidence." }
        val chunks = DocumentTextChunker.chunk(text, maxChars = 180)

        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 180 })
        assertEquals(text, chunks.joinToString("\n\n"))
    }

    @Test
    fun overlapRepeatsBoundaryContextWithoutDroppingTail() {
        val text = "abcdefghijklmnopqrstuvwxyz"
        val chunks = DocumentTextChunker.chunk(text, maxChars = 10, overlapChars = 2)

        assertEquals(listOf("abcdefghij", "ijklmnopqr", "qrstuvwxyz"), chunks)
        assertTrue(chunks.last().endsWith("xyz"))
    }

    @Test
    fun sourceAwareChunksExposeExactRanges() {
        val chunks =
            DocumentTextChunker.chunkWithOffsets(
                "abcdefghijklmnopqrstuvwxyz",
                maxChars = 10,
                overlapChars = 2,
            )

        assertEquals(3, chunks.size)
        assertEquals(0, chunks[0].startOffset)
        assertEquals(10, chunks[0].endOffsetExclusive)
        assertEquals("Chunk 1 · chars 0-9", chunks[0].sourceLabel)
        assertEquals(8, chunks[1].startOffset)
        assertEquals(18, chunks[1].endOffsetExclusive)
        assertEquals("ijklmnopqr", chunks[1].text)
        assertEquals(16, chunks[2].startOffset)
        assertEquals(26, chunks[2].endOffsetExclusive)
        assertEquals("qrstuvwxyz", chunks[2].text)
    }

    @Test
    fun sourceAwareOffsetsRebuildEachChunkFromNormalizedText() {
        val text = "alpha beta gamma delta epsilon"
        val normalized = text.trim()
        val chunks = DocumentTextChunker.chunkWithOffsets(text, maxChars = 12, overlapChars = 3)

        chunks.forEach { chunk ->
            assertEquals(
                chunk.text,
                normalized.substring(chunk.startOffset, chunk.endOffsetExclusive).trim(),
            )
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverlapAsLargeAsChunk() {
        DocumentTextChunker.chunk("abc", maxChars = 10, overlapChars = 10)
    }
}
