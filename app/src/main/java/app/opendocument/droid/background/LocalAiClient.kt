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
        val chunks = DocumentTextChunker.chunk(documentText, MAX_CHUNK_CHARS, CHUNK_OVERLAP_CHARS)
        if (chunks.isEmpty()) return Result.Failure("AI tidak menemui teks untuk dianalisis.")
        if (chunks.size == 1) return request(chunks.single(), instruction)

        val evidence = mutableListOf<String>()
        chunks.forEachIndexed { index, chunk ->
            val chunkInstruction =
                "$instruction\n\n" +
                    "This is document chunk ${index + 1} of ${chunks.size}. Extract only evidence " +
                    "from this chunk that is relevant to the instruction. Preserve names, dates, " +
                    "figures, obligations and page-like headings when present. Do not invent facts."
            when (val result = request(chunk, chunkInstruction)) {
                is Result.Success -> evidence += "Chunk ${index + 1}: ${result.answer}"
                is Result.Failure -> return result
            }
        }

        val condensed = condenseEvidence(evidence.joinToString("\n\n"), instruction)
        if (condensed is Result.Failure) return condensed

        return request(
            (condensed as Result.Success).answer,
            "$instruction\n\nUse the extracted evidence below to produce one final answer. " +
                "Do not add facts that are absent from the evidence.",
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
                    val result = request(
                        chunk,
                        "Condense this extracted document evidence for the user's instruction: " +
                            "$instruction\nKeep only supported facts, names, dates, figures, risks, " +
                            "obligations and action items. This is evidence block ${index + 1} of " +
                            "${chunks.size}.",
                    )
                ) {
                    is Result.Success -> reduced += result.answer
                    is Result.Failure -> return result
                }
            }
            current = reduced.joinToString("\n\n")
        }

        return if (current.length <= MAX_CHUNK_CHARS) Result.Success(current)
        else Result.Failure("Dokumen terlalu panjang untuk diringkaskan dengan selamat.")
    }

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
