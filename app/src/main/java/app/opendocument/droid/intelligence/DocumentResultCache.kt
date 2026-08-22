package app.opendocument.droid.intelligence

import java.util.LinkedHashMap

/** Small in-memory LRU cache for repeated document actions. */
class DocumentResultCache(
    private val maxEntries: Int = 48,
) {
    private val cache = object : LinkedHashMap<String, DocumentIntelligenceResult>(16, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, DocumentIntelligenceResult>?,
        ): Boolean = size > maxEntries
    }

    @Synchronized
    fun get(key: String): DocumentIntelligenceResult? = cache[key]

    @Synchronized
    fun put(key: String, value: DocumentIntelligenceResult) {
        cache[key] = value
    }

    @Synchronized
    fun invalidateDocument(documentId: String) {
        val prefix = "$documentId:"
        cache.keys.removeAll { it.startsWith(prefix) }
    }

    @Synchronized
    fun clear() {
        cache.clear()
    }

    companion object {
        fun keyFor(request: DocumentIntelligenceRequest): String {
            val textFingerprint = 31 * request.documentText.length + request.documentText.hashCode()
            return buildString {
                append(request.documentId)
                append(':')
                append(request.action.name)
                append(':')
                append(textFingerprint)
                append(':')
                append(request.userQuestion.orEmpty().hashCode())
                append(':')
                append(request.selectedText.orEmpty().hashCode())
            }
        }
    }
}
