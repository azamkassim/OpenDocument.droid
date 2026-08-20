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

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverlapAsLargeAsChunk() {
        DocumentTextChunker.chunk("abc", maxChars = 10, overlapChars = 10)
    }
}
