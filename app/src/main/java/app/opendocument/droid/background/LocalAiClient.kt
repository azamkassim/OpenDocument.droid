package app.opendocument.droid.background

import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject

/** Talks only to the OpenAI-compatible model server running locally in Termux. */
class LocalAiClient(private val endpoint: String = "http://localhost:8081/v1/chat/completions") {
    sealed interface Result {
        data class Success(val answer: String) : Result

        data class Failure(val message: String) : Result
    }

    fun ask(documentText: String, instruction: String, callback: (Result) -> Unit) {
        Thread {
            val result = runCatching {
                analyseDocument(documentText, instruction)
            }
                .getOrElse { error ->
                    Result.Failure(
                        if (error is IOException) {
                            "AI offline belum berjalan. Buka Termux dan jalankan enjin AI, " +
                                "kemudian cuba semula."
                        } else {
                            error.message ?: "AI tidak dapat memproses dokumen ini."
                        }
                    )
                }

            Handler(Looper.getMainLooper()).post { callback(result) }
        }
            .start()
    }

    private fun analyseDocument(documentText: String, instruction: String): Result {
        val chunks =
            DocumentTextChunker.chunkWithOffsets(
                documentText,
                MAX_CHUNK_CHARS,
                CHUNK_OVERLAP_CHARS,
            )
        if (chunks.isEmpty()) return Result.Failure("AI tidak menemui teks untuk dianalisis.")

        if (chunks.size == 1) {
            val chunk = chunks.single()
            return request(
                chunk.text,
                "$instruction\n\nSource for this document text: ${sourceTag(chunk)}. " +
                    "Cite this source marker for supported claims.",
            )
        }

        val evidence = mutableListOf<String>()
        chunks.forEach { chunk ->
            val sourceTag = sourceTag(chunk)
            val chunkInstruction =
                "$instruction\n\n" +
                    "Extract only supported findings from this source block: $sourceTag. " +
                    "Return one finding per line using exactly: CATEGORY | $sourceTag | finding. " +
                    "CATEGORY must be SUMMARY, RISK, OBLIGATION, DATE, FIGURE, or ACTION_ITEM. " +
                    "Preserve names, dates, figures and obligations. Do not invent facts or sources."
            when (val result = request(chunk.text, chunkInstruction)) {
                is Result.Success -> evidence += result.answer
                is Result.Failure -> return result
            }
        }

        val condensed = condenseEvidence(evidence.joinToString("\n"), instruction)
        if (condensed is Result.Failure) return condensed

        val condensedText = (condensed as Result.Success).answer
        val structured = StructuredDocumentFindingsParser.parse(condensedText)
        val finalEvidence = if (structured.isEmpty()) condensedText else structured.formatForModel()

        return request(
            finalEvidence,
            "$instruction\n\nUse only the structured extracted evidence below to produce one final answer. " +
                "Cover relevant summary, risks, obligations, dates, figures, and action items. " +
                "Cite the relevant [SOURCE ...] marker after each supported claim or bullet. " +
                "Never invent page numbers or source markers, and do not add unsupported facts.",
        )
    }

    private fun condenseEvidence(evidence: String, instruction: String): Result {
        var current = evidence
        repeat(MAX_REDUCTION_ROUNDS) {
            if (current.length <= MAX_CHUNK_CHARS) return Result.Success(current)

            val reduced = mutableListOf<String>()
            val chunks = DocumentTextChunker.chunk(current, MAX_CHUNK_CHARS)
            chunks.forEachIndexed { index, chunk ->
                when (
                    val result =
                        request(
                            chunk,
                            "Condense these structured findings for the user's instruction: " +
                                "$instruction\nKeep only supported findings. Preserve the line format " +
                                "CATEGORY | [SOURCE ...] | finding and every source marker verbatim. " +
                                "Allowed categories: SUMMARY, RISK, OBLIGATION, DATE, FIGURE, " +
                                "ACTION_ITEM. This is evidence block ${index + 1} of ${chunks.size}.",
                        )
                ) {
                    is Result.Success -> reduced += result.answer
                    is Result.Failure -> return result
                }
            }
            current = reduced.joinToString("\n")
        }

        return if (current.length <= MAX_CHUNK_CHARS) Result.Success(current)
        else Result.Failure("Dokumen terlalu panjang untuk diringkaskan dengan selamat.")
    }

    private fun sourceTag(chunk: DocumentTextChunker.TextChunk): String =
        "[SOURCE ${chunk.sourceLabel}]"

    private fun request(documentText: String, instruction: String): Result {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

        val messages =
            JSONArray()
                .put(
                    JSONObject()
                        .put("role", "system")
                        .put(
                            "content",
                            "You are a private offline document assistant. Answer only from the " +
                                "document supplied. If the answer is absent, say so clearly. " +
                                "Reply in the same language as the user's instruction.",
                        )
                )
                .put(
                    JSONObject()
                        .put("role", "user")
                        .put("content", "$instruction\n\nDOCUMENT:\n$documentText")
                )

        val body =
            JSONObject()
                .put("model", "local-model")
                .put("messages", messages)
                .put("temperature", 0.2)
                .put("max_tokens", 256)
                .put("stream", false)
                .toString()

        connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }

        val status = connection.responseCode
        val responseStream =
            if (status in 200..299) connection.inputStream else connection.errorStream
        val response =
            responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        connection.disconnect()

        if (status !in 200..299) {
            return Result.Failure(
                "Enjin AI memberi ralat $status. Cuba mulakan semula enjin di Termux."
            )
        }

        val answer =
            JSONObject(response)
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()

        return if (answer.isEmpty()) Result.Failure("AI tidak menghasilkan jawapan.")
        else Result.Success(answer)
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 5_000
        private const val READ_TIMEOUT_MS = 300_000
        private const val MAX_CHUNK_CHARS = 3_000
        private const val CHUNK_OVERLAP_CHARS = 160
        private const val MAX_REDUCTION_ROUNDS = 6
    }
}
