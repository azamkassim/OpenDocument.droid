package app.opendocument.droid.intelligence

/**
 * Lightweight coordinator that keeps common intelligence actions responsive.
 *
 * It samples large documents for fast actions, reuses recent results, and never mutates the original document
 * text. The caller remains responsible for executing this suspend function away from the Android main thread.
 */
class FastDocumentIntelligenceCoordinator(
    private val engine: DocumentIntelligenceEngine,
    private val policy: PerformancePolicy = NexusDefaultPerformancePolicy.value,
) : DocumentIntelligenceEngine {
    private val cache =
        object : LinkedHashMap<CacheKey, DocumentIntelligenceResult>(policy.maxCachedResults, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<CacheKey, DocumentIntelligenceResult>?,
            ): Boolean = size > policy.maxCachedResults
        }

    override suspend fun execute(request: DocumentIntelligenceRequest): DocumentIntelligenceResult {
        val key = request.cacheKey()

        if (policy.cacheDerivedResults) {
            synchronized(cache) {
                cache[key]?.let { return it }
            }
        }

        val prepared = prepare(request)
        val result = engine.execute(prepared)
        val bounded = result.limit(policy)

        if (policy.cacheDerivedResults) {
            synchronized(cache) {
                cache[key] = bounded
            }
        }
        return bounded
    }

    fun clearCache() {
        synchronized(cache) {
            cache.clear()
        }
    }

    fun prewarm(request: DocumentIntelligenceRequest): DocumentIntelligenceRequest = prepare(request)

    private fun prepare(request: DocumentIntelligenceRequest): DocumentIntelligenceRequest {
        if (!isFastPath(request.action) || request.documentText.length <= policy.fastPathMaxChars) {
            return request
        }

        return request.copy(
            documentText =
                DocumentTextWindowing.representativeSample(
                    text = request.documentText,
                    maxChars = policy.fastPathMaxChars,
                    windows = policy.sampleWindows,
                ),
        )
    }

    private fun isFastPath(action: DocumentAction): Boolean =
        action == DocumentAction.SUMMARISE ||
            action == DocumentAction.IDENTIFY_DOCUMENT ||
            action == DocumentAction.FIND_KEY_RISKS ||
            action == DocumentAction.FIND_ACTION_ITEMS ||
            action == DocumentAction.FIND_DATES_AND_DEADLINES

    private fun DocumentIntelligenceRequest.cacheKey(): CacheKey =
        CacheKey(
            action = action,
            documentId = documentId,
            fingerprint = fastDocumentFingerprint(documentText),
            question = userQuestion.orEmpty(),
            selection = selectedText.orEmpty(),
        )

    private fun DocumentIntelligenceResult.limit(policy: PerformancePolicy): DocumentIntelligenceResult =
        copy(
            evidence = evidence.take(policy.maxEvidenceItems),
            extractedFields = extractedFields.take(policy.maxExtractedFields),
        )

    private data class CacheKey(
        val action: DocumentAction,
        val documentId: String,
        val fingerprint: Int,
        val question: String,
        val selection: String,
    )
}
