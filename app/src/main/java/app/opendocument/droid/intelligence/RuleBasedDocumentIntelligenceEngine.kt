package app.opendocument.droid.intelligence

/**
 * Small dependency-free fallback engine used while the local LLM/Collabora adapters
 * are being wired. It never claims to understand more than it can verify from text.
 */
class RuleBasedDocumentIntelligenceEngine : DocumentIntelligenceEngine {
    override suspend fun execute(request: DocumentIntelligenceRequest): DocumentIntelligenceResult {
        val text = request.documentText.trim()
        if (text.isEmpty()) {
            return DocumentIntelligenceResult(
                title = "No document text",
                answer = "No readable text was available for analysis.",
                confidence = Confidence.HIGH,
                warnings = listOf("The renderer or extractor did not provide document text."),
            )
        }

        return when (request.action) {
            DocumentAction.IDENTIFY_DOCUMENT -> identify(text)
            DocumentAction.FIND_DATES_AND_DEADLINES -> findDates(text)
            DocumentAction.FIND_ACTION_ITEMS -> findActionItems(text)
            DocumentAction.FIND_KEY_RISKS -> findRisks(text)
            DocumentAction.SUMMARISE -> summarise(text)
            else -> DocumentIntelligenceResult(
                title = request.action.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                answer = "This action requires the local AI or specialist parser adapter.",
                confidence = Confidence.HIGH,
                warnings = listOf("No model-generated answer was produced."),
                suggestedNextActions = listOf(DocumentAction.SUMMARISE, DocumentAction.IDENTIFY_DOCUMENT),
            )
        }
    }

    private fun identify(text: String): DocumentIntelligenceResult {
        val lower = text.lowercase()
        val type = when {
            listOf("balance sheet", "income statement", "profit before tax", "revenue").count { it in lower } >= 2 -> "Financial statements"
            listOf("facility", "financing", "tenure", "security", "conditions precedent").count { it in lower } >= 2 -> "Financing / credit document"
            listOf("agreement", "party", "termination", "effective date").count { it in lower } >= 2 -> "Contract / agreement"
            listOf("invoice", "amount due", "tax invoice").count { it in lower } >= 2 -> "Invoice"
            else -> "General document"
        }

        return DocumentIntelligenceResult(
            title = "Document type",
            answer = type,
            confidence = if (type == "General document") Confidence.LOW else Confidence.MEDIUM,
            suggestedNextActions = listOf(DocumentAction.SUMMARISE, DocumentAction.FIND_KEY_RISKS),
        )
    }

    private fun summarise(text: String): DocumentIntelligenceResult {
        val sentences = text
            .replace("\n", " ")
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length >= 30 }
            .take(5)

        val answer = if (sentences.isEmpty()) text.take(600) else sentences.joinToString(" ")
        return DocumentIntelligenceResult(
            title = "Quick summary",
            answer = answer,
            confidence = Confidence.MEDIUM,
            warnings = listOf("Fallback extractive summary; local AI summary not used."),
        )
    }

    private fun findDates(text: String): DocumentIntelligenceResult {
        val dateRegex = Regex("\\b(?:[0-3]?\\d[-/][01]?\\d[-/](?:19|20)?\\d{2}|[0-3]?\\d\\s+(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+(?:19|20)\\d{2})\\b", RegexOption.IGNORE_CASE)
        val matches = dateRegex.findAll(text).map { it.value }.distinct().take(20).toList()
        return DocumentIntelligenceResult(
            title = "Dates found",
            answer = if (matches.isEmpty()) "No explicit dates were found." else matches.joinToString("\n"),
            confidence = Confidence.HIGH,
            extractedFields = matches.map { ExtractedField("date", it, Confidence.HIGH) },
        )
    }

    private fun findActionItems(text: String): DocumentIntelligenceResult {
        val candidates = text.lines()
            .map { it.trim() }
            .filter { line ->
                val lower = line.lowercase()
                line.length in 20..300 && listOf("shall", "must", "required", "submit", "provide", "complete", "before", "within").any { it in lower }
            }
            .take(12)

        return DocumentIntelligenceResult(
            title = "Possible actions / obligations",
            answer = if (candidates.isEmpty()) "No explicit action items were detected." else candidates.joinToString("\n• ", prefix = "• "),
            confidence = Confidence.MEDIUM,
            warnings = listOf("Rule-based detection; review the source wording before acting."),
        )
    }

    private fun findRisks(text: String): DocumentIntelligenceResult {
        val keywords = listOf("default", "termination", "breach", "penalty", "overdue", "liability", "waiver", "covenant", "shortfall", "adverse")
        val hits = text.lines()
            .map { it.trim() }
            .filter { line -> keywords.any { it in line.lowercase() } }
            .filter { it.length in 20..400 }
            .take(12)

        return DocumentIntelligenceResult(
            title = "Potential risk signals",
            answer = if (hits.isEmpty()) "No obvious keyword-based risk signals were found." else hits.joinToString("\n• ", prefix = "• "),
            confidence = Confidence.LOW,
            warnings = listOf("This is a screening aid, not a credit/legal conclusion."),
            suggestedNextActions = listOf(DocumentAction.ASK_DOCUMENT, DocumentAction.CHECK_INCONSISTENCIES),
        )
    }
}
