package com.kuikly.kuiklychat.dsl.chat

import android.os.SystemClock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

/** Minimal streaming transport for the Android demo; production apps should use a server proxy. */
internal actual fun streamKimiA2ui(
    apiKey: String,
    model: String,
    prompt: String,
    history: List<KimiTurn>,
): Flow<String> = callbackFlow {
    logAgentCardDemo("llm_prompt", A2UI_DEMO_SYSTEM_PROMPT)
    logAgentCardDemo("user_prompt", prompt)
    val executor = Executors.newSingleThreadExecutor()
    val connection = AtomicReference<HttpURLConnection?>(null)
    val task = AtomicReference<Future<*>?>(null)

    task.set(executor.submit {
        var input: java.io.InputStream? = null
        try {
            val request = JSONObject()
                .put("model", model)
                .put("stream", true)
                .put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", A2UI_DEMO_SYSTEM_PROMPT))
                    history.forEach { turn -> put(JSONObject().put("role", turn.role).put("content", turn.content)) }
                    put(JSONObject().put("role", "user").put("content", prompt))
                })
            applyKimiSpeedOptions(request, model)

            logAgentCardDemo("llm_request_json", request.toString())

            val http = (URL("https://api.moonshot.cn/v1/chat/completions").openConnection() as HttpURLConnection).also {
                connection.set(it)
                it.requestMethod = "POST"
                it.doOutput = true
                it.connectTimeout = 15_000
                it.readTimeout = 90_000
                it.setRequestProperty("Authorization", "Bearer $apiKey")
                it.setRequestProperty("Content-Type", "application/json")
                it.setRequestProperty("Accept", "text/event-stream")
            }
            http.outputStream.use { output ->
                output.write(request.toString().toByteArray(Charsets.UTF_8))
                output.flush()
            }

            val status = http.responseCode
            if (status !in 200..299) {
                val error = http.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                close(IllegalStateException("Kimi 请求失败（HTTP $status）：${error.take(300)}"))
                return@submit
            }

            input = http.inputStream
            val buffer = CharArray(1024)
            val reader = input!!.bufferedReader(Charsets.UTF_8)
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                if (count > 0 && !trySend(String(buffer, 0, count)).isSuccess) break
                SystemClock.sleep(1)
            }
            close()
        } catch (cancelled: InterruptedException) {
            // Cancellation closes the HTTP connection below.
        } catch (error: Throwable) {
            close(error)
        } finally {
            input?.close()
            connection.getAndSet(null)?.disconnect()
        }
    })

    awaitClose {
        connection.getAndSet(null)?.disconnect()
        task.getAndSet(null)?.cancel(true)
        executor.shutdownNow()
    }
}

/**
 * Card generation is mostly about output speed. K3's default `reasoning_effort=max`
 * thinks for 60s+ before the first JSONL line; `low` starts writing after ~5s.
 * K2.x can turn thinking off entirely. Each model also pins its own temperature.
 */
private fun applyKimiSpeedOptions(request: JSONObject, model: String) {
    when {
        model.startsWith("kimi-k3") -> request
            .put("temperature", 1)
            .put("reasoning_effort", "low")
        model == "kimi-k2.6" || model == "kimi-k2.5" -> request
            .put("temperature", 0.6)
            .put("thinking", JSONObject().put("type", "disabled"))
        else -> request.put("reasoning_effort", "low")
    }
}
