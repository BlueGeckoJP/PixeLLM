package me.bluegecko.pixellm.routes

import android.util.Log
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.post
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.bluegecko.pixellm.ChatCompletions
import me.bluegecko.pixellm.LlmManager

fun Route.chatRoutes(
    llmManager: LlmManager,
    getLoadDeferred: () -> Deferred<Unit>?,
    json: Json = Json { ignoreUnknownKeys = true },
) {
    post("/v1/chat/completions") {
        try {
            getLoadDeferred()?.await()

            val rawRequest = call.receiveText()
            val request = json.decodeFromString<ChatCompletions>(rawRequest)
            val messages = request.messages.joinToString(separator = "\n") {
                "${it.role}: ${it.content}"
            }

            Log.d("ChatRoutes", "Received raw request: $rawRequest")
            Log.d("ChatRoutes", "Parsed object: $request")
            Log.d("ChatRoutes", "Received prompt: $messages")

            if (request.stream == false) {
                handleNonStreamingResponse(call, llmManager, messages)
            } else {
                handleStreamingResponse(call, llmManager, messages)
            }
        } catch (e: Throwable) {
            Log.e("ChatRoutes", "Error handling chat completion request", e)
            call.respondText(
                text = "Error: ${e.message}",
                status = HttpStatusCode.ServiceUnavailable
            )
        }
    }
}

suspend fun handleNonStreamingResponse(call: RoutingCall, llmManager: LlmManager, prompt: String) {
    val response = llmManager.generate(prompt)
    call.respondText(
        text = "data: {\"choices\":[{\"delta\":{\"content\":${Json.encodeToString(response)}}}]}\n\n",
        contentType = ContentType.Application.Json
    )
}

suspend fun handleStreamingResponse(
    call: RoutingCall,
    llmManager: LlmManager,
    messages: String,
) {
    val channel = Channel<String>(Channel.UNLIMITED)

    call.respondTextWriter(ContentType.Text.EventStream) {
        coroutineScope {
            val generationJob = launch {
                llmManager.generateAsync(messages, channel)
            }

            try {
                for (chunk in channel) {
                    write(
                        "data: {\"choices\":[{\"delta\":{\"content\":${Json.encodeToString(chunk)}}}]}\n\n"
                    )
                    flush()
                }

                write("data: [DONE]\n\n")
                flush()
            } finally {
                generationJob.cancel()
                channel.close()
            }
        }
    }
}