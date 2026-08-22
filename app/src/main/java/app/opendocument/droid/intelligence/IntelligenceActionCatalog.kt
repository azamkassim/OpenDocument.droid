package app.opendocument.droid.intelligence

/** Small, UI-friendly catalog for the future LibreOffice/Collabora Intelligence sheet. */
data class IntelligenceActionItem(
    val action: DocumentAction,
    val title: String,
    val description: String,
    val priority: Int,
)

object IntelligenceActionCatalog {
    val primary =
        listOf(
            IntelligenceActionItem(
                DocumentAction.SUMMARISE,
                "Summarise",
                "Get the important points with evidence.",
                1,
            ),
            IntelligenceActionItem(
                DocumentAction.ASK_DOCUMENT,
                "Ask Document",
                "Ask questions grounded only in this document.",
                2,
            ),
            IntelligenceActionItem(
                DocumentAction.FIND_KEY_RISKS,
                "Key Risks",
                "Find risks, unusual clauses and missing information.",
                3,
            ),
            IntelligenceActionItem(
                DocumentAction.FIND_ACTION_ITEMS,
                "Actions & Deadlines",
                "Find obligations, dates and next steps.",
                4,
            ),
            IntelligenceActionItem(
                DocumentAction.EXTRACT_DATA,
                "Extract Data",
                "Turn document facts into structured fields.",
                5,
            ),
            IntelligenceActionItem(
                DocumentAction.CHECK_INCONSISTENCIES,
                "Check Consistency",
                "Flag conflicting figures, dates or names.",
                6,
            ),
        )

    val selection =
        listOf(
            IntelligenceActionItem(
                DocumentAction.EXPLAIN_SELECTION,
                "Explain",
                "Explain selected text in plain language.",
                1,
            ),
            IntelligenceActionItem(
                DocumentAction.REWRITE_SELECTION,
                "Rewrite",
                "Improve clarity without changing factual meaning.",
                2,
            ),
            IntelligenceActionItem(
                DocumentAction.REDACT_SENSITIVE_DATA,
                "Redact",
                "Preview likely sensitive content before redaction.",
                3,
            ),
        )
}
