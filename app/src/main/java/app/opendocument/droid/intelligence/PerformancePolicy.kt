package app.opendocument.droid.intelligence

/**
 * Performance guardrails for document intelligence.
 *
 * Common actions should feel instant on mobile, while heavier work must never
 * block document rendering, scrolling, editing, selection, or save.
 */
data class PerformancePolicy(
    val fastPathMaxChars: Int = 12_000,
    val previewMaxChars: Int = 2_000,
    val maxEvidenceItems: Int = 12,
    val maxExtractedFields: Int = 64,
    val debounceMs: Long = 120,
    val uiBudgetMs: Long = 16,
    val quickActionBudgetMs: Long = 350,
    val backgroundActionBudgetMs: Long = 3_000,
    val cancelSupersededWork: Boolean = true,
    val cacheDocumentText: Boolean = true,
    val cacheDerivedResults: Boolean = true,
    val preferIncrementalAnalysis: Boolean = true,
    val prewarmOnDocumentOpen: Boolean = true,
)

object NexusDefaultPerformancePolicy {
    val value = PerformancePolicy()
}
