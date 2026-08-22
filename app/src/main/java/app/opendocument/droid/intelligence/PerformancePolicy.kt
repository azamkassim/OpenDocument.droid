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
    val maxCachedResults: Int = 24,
    val debounceMs: Long = 80,
    val uiBudgetMs: Long = 16,
    val quickActionBudgetMs: Long = 250,
    val backgroundActionBudgetMs: Long = 3_000,
    val cancelSupersededWork: Boolean = true,
    val cacheDocumentText: Boolean = true,
    val cacheDerivedResults: Boolean = true,
    val preferIncrementalAnalysis: Boolean = true,
    val prewarmOnDocumentOpen: Boolean = true,
) {
    init {
        require(fastPathMaxChars > 0)
        require(previewMaxChars in 1..fastPathMaxChars)
        require(maxEvidenceItems > 0)
        require(maxExtractedFields > 0)
        require(maxCachedResults > 0)
        require(debounceMs >= 0)
        require(uiBudgetMs in 1..32)
        require(quickActionBudgetMs >= uiBudgetMs)
        require(backgroundActionBudgetMs >= quickActionBudgetMs)
    }
}

object NexusDefaultPerformancePolicy {
    val value = PerformancePolicy()
}

/**
 * Fast, non-cryptographic fingerprint for in-memory result caching. It samples long
 * documents so cache lookup stays cheap. Never use this as a security hash.
 */
fun fastDocumentFingerprint(text: String): Int {
    var hash = 17
    val step = (text.length / 128).coerceAtLeast(1)
    var index = 0
    while (index < text.length) {
        hash = 31 * hash + text[index].code
        index += step
    }
    return 31 * hash + text.length
}
