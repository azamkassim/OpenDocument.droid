package app.opendocument.droid.intelligence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentTextWindowingTest {
    @Test
    fun representativeSampleCoversBeginningMiddleAndEnd() {
        val text = "BEGIN-" + "a".repeat(4_000) + "-MIDDLE-" + "b".repeat(4_000) + "-END"

        val sample = DocumentTextWindowing.representativeSample(text, maxChars = 1_500, windows = 3)

        assertTrue(sample.length <= 1_500)
        assertTrue(sample.contains("BEGIN-"))
        assertTrue(sample.contains("-END"))
    }

    @Test
    fun chunksCoverWholeDocumentWithOverlap() {
        val text = "x".repeat(2_500)
        val chunks = DocumentTextWindowing.chunks(text, chunkChars = 1_000, overlapChars = 100).toList()

        assertEquals(3, chunks.size)
        assertEquals(0, chunks.first().start)
        assertEquals(text.length, chunks.last().endExclusive)
        assertEquals(900, chunks[1].start)
    }
}
