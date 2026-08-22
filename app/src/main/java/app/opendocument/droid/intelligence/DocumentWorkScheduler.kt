package app.opendocument.droid.intelligence

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight coordination for document analysis jobs.
 *
 * A new request for the same document/action invalidates the older request so
 * callers can cheaply discard stale work instead of letting it reach the UI.
 */
class DocumentWorkScheduler {
    private val generations = ConcurrentHashMap<String, AtomicLong>()

    fun begin(documentId: String, action: DocumentAction): WorkTicket {
        val key = "$documentId:${action.name}"
        val generation = generations.getOrPut(key) { AtomicLong(0) }.incrementAndGet()
        return WorkTicket(key, generation)
    }

    fun isCurrent(ticket: WorkTicket): Boolean =
        generations[ticket.key]?.get() == ticket.generation

    fun invalidate(documentId: String, action: DocumentAction) {
        val key = "$documentId:${action.name}"
        generations.getOrPut(key) { AtomicLong(0) }.incrementAndGet()
    }

    fun clear() {
        generations.clear()
    }
}

data class WorkTicket(
    val key: String,
    val generation: Long,
)
