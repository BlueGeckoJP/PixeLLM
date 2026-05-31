package me.bluegecko.pixellm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LocalLlmService(
    private val context: Context,
    private val modelPath: String
) : Closeable {
    private val mutex = Mutex()
    private var llm: LlmInference? = null
    private var llmSession: LlmInferenceSession? = null

    suspend fun load() = withContext(Dispatchers.Default) {
        mutex.withLock {
            if (llmSession != null) return@withLock

            val llmOpts = LlmInference.LlmInferenceOptions.builder().setModelPath(modelPath)
                .setMaxTokens(1024).setMaxTopK(40).build()
            llm = LlmInference.createFromOptions(context, llmOpts)

            val sessionOpts = LlmInferenceSession.LlmInferenceSessionOptions.builder().build()
            llmSession = LlmInferenceSession.createFromOptions(llm, sessionOpts)
        }
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.Default) {
        mutex.withLock {
            val engine = requireNotNull(llm) {
                "LlmInference model is not loaded. Call load() before generating."
            }

            engine.generateResponse(prompt)
        }
    }

    suspend fun generateAsync(prompt: String, channel: Channel<String>): Unit =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                val session = requireNotNull(llmSession) {
                    "LlmInferenceSession is not initialized. Call load() before generating."
                }

                session.addQueryChunk(prompt)
                session.generateResponseAsync { partialResult, done ->
                    Log.d("LLM", "chunk=$partialResult, done=$done")
                    if (partialResult != null) {
                        channel.trySend(partialResult)
                    }

                    if (done) {
                        Log.d("LLM", "finished")
                        channel.close()
                    }
                }
            }
        }

    override fun close() {
        llm?.close()
        llm = null
    }
}