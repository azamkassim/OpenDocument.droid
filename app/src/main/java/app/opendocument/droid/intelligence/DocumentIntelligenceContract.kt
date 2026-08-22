package app.opendocument.droid.intelligence

/**
 * Engine-neutral contract for the NEXUS Document Intelligence layer.
 *
 * The office renderer/editor (LibreOffice/Collabora) is intentionally kept
 * separate from analysis so this layer can be reused across Android office
 * engines without coupling banking logic to the renderer.
 */
interface DocumentIntelligenceEngine {
    suspend fun execute(request: DocumentIntelligenceRequest): DocumentIntelligenceResult
}

data class DocumentIntelligenceRequest(
    val action: DocumentAction,
    val documentId: String,
    val documentText: String,
    val userQuestion: String? = null,
    val selectedText: String? = null,
    val documentTypeHint: String? = null,
    val localOnly: Boolean = true,
)

data class DocumentIntelligenceResult(
    val title: String,
    val answer: String,
    val confidence: Confidence,
    val evidence: List<EvidenceRef> = emptyList(),
    val extractedFields: List<ExtractedField> = emptyList(),
    val warnings: List<String> = emptyList(),
    val suggestedNextActions: List<DocumentAction> = emptyList(),
)

data class EvidenceRef(
    val label: String,
    val excerpt: String,
    val page: Int? = null,
    val section: String? = null,
)

data class ExtractedField(
    val name: String,
    val value: String,
    val confidence: Confidence,
    val source: EvidenceRef? = null,
)

enum class Confidence { LOW, MEDIUM, HIGH }

enum class DocumentAction {
    SUMMARISE,
    ASK_DOCUMENT,
    IDENTIFY_DOCUMENT,
    EXTRACT_DATA,
    ANALYSE,
    FIND_KEY_RISKS,
    FIND_ACTION_ITEMS,
    FIND_DATES_AND_DEADLINES,
    COMPARE_DOCUMENTS,
    CHECK_INCONSISTENCIES,
    EXPLAIN_SELECTION,
    REWRITE_SELECTION,
    REDACT_SENSITIVE_DATA,
    PREPARE_CAR_FACTS,
    EXTRACT_FINANCIAL_FACTS,
}
