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
                request(documentText, instruction)
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
                        .put(
                            "content",
                            "$instruction\n\nDOCUMENT:\n${documentText.take(MAX_DOCUMENT_CHARS)}",
                        )
                )

        val body =
            JSONObject()
                .put("model", "local-model")
                .put("messages", messages)
                .put("temperature", 0.2)
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
        private const val MAX_DOCUMENT_CHARS = 8_000
    }
}
