package app.opendocument.droid.intelligence

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedDocumentIntelligenceEngineTest {
    private val engine = RuleBasedDocumentIntelligenceEngine()

    @Test
    fun identifiesFinancialStatements() {
        val result = runSuspend {
            engine.execute(
                DocumentIntelligenceRequest(
                    action = DocumentAction.IDENTIFY_DOCUMENT,
                    documentId = "financials",
                    documentText = "Revenue increased. Profit before tax improved. Balance sheet remains stable.",
                ),
            )
        }

        assertEquals("Financial statements", result.answer)
        assertTrue(result.confidence == Confidence.MEDIUM || result.confidence == Confidence.HIGH)
    }

    @Test
    fun emptyDocumentReturnsGroundedWarning() {
        val result = runSuspend {
            engine.execute(
                DocumentIntelligenceRequest(
                    action = DocumentAction.SUMMARISE,
                    documentId = "empty",
                    documentText = "   ",
                ),
            )
        }

        assertEquals("No document text", result.title)
        assertEquals(Confidence.HIGH, result.confidence)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun unsupportedActionDoesNotInventAnswer() {
        val result = runSuspend {
            engine.execute(
                DocumentIntelligenceRequest(
                    action = DocumentAction.PREPARE_CAR_FACTS,
                    documentId = "credit",
                    documentText = "Facility amount RM40 million.",
                ),
            )
        }

        assertTrue(result.answer.contains("requires the local AI or specialist parser adapter"))
        assertTrue(result.warnings.contains("No model-generated answer was produced."))
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var outcome: Result<T>? = null
        block.startCoroutine(
            object : Continuation<T> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<T>) {
                    outcome = result
                }
            },
        )
        return checkNotNull(outcome).getOrThrow()
    }
}
