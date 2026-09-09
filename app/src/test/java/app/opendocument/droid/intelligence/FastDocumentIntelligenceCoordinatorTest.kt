package app.opendocument.droid.intelligence

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FastDocumentIntelligenceCoordinatorTest {
    @Test
    fun fastPathTruncatesVeryLargeInput() = runBlocking {
        var seenLength = 0
        val engine =
            object : DocumentIntelligenceEngine {
                override suspend fun execute(
                    request: DocumentIntelligenceRequest,
                ): DocumentIntelligenceResult {
                    seenLength = request.documentText.length
                    return DocumentIntelligenceResult("ok", "ok", Confidence.HIGH)
                }
            }
        val policy = PerformancePolicy(fastPathMaxChars = 1000, previewMaxChars = 500)
        val coordinator = FastDocumentIntelligenceCoordinator(engine, policy)

        coordinator.execute(
            DocumentIntelligenceRequest(
                action = DocumentAction.SUMMARISE,
                documentId = "large",
                documentText = "x".repeat(50_000),
            ),
        )

        assertEquals(1000, seenLength)
    }

    @Test
    fun repeatedRequestUsesCache() = runBlocking {
        var calls = 0
        val engine =
            object : DocumentIntelligenceEngine {
                override suspend fun execute(
                    request: DocumentIntelligenceRequest,
                ): DocumentIntelligenceResult {
                    calls += 1
                    return DocumentIntelligenceResult("ok", request.documentText, Confidence.HIGH)
                }
            }
        val coordinator = FastDocumentIntelligenceCoordinator(engine)
        val request =
            DocumentIntelligenceRequest(
                action = DocumentAction.IDENTIFY_DOCUMENT,
                documentId = "same",
                documentText = "same content",
            )

        coordinator.execute(request)
        coordinator.execute(request)

        assertEquals(1, calls)
    }

    @Test
    fun resultSizeIsBounded() = runBlocking {
        val engine =
            object : DocumentIntelligenceEngine {
                override suspend fun execute(
                    request: DocumentIntelligenceRequest,
                ): DocumentIntelligenceResult =
                    DocumentIntelligenceResult(
                        title = "bounded",
                        answer = "ok",
                        confidence = Confidence.HIGH,
                        evidence = List(50) { EvidenceRef("e$it", "x") },
                        extractedFields =
                            List(100) {
                                ExtractedField("f$it", "$it", Confidence.HIGH)
                            },
                    )
            }
        val policy = PerformancePolicy(maxEvidenceItems = 5, maxExtractedFields = 7)
        val result =
            FastDocumentIntelligenceCoordinator(engine, policy).execute(
                DocumentIntelligenceRequest(
                    action = DocumentAction.SUMMARISE,
                    documentId = "bounded",
                    documentText = "text",
                ),
            )

        assertEquals(5, result.evidence.size)
        assertEquals(7, result.extractedFields.size)
        assertTrue(result.answer.isNotEmpty())
    }
}
