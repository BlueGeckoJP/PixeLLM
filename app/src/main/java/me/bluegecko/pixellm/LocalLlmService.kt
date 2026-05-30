package me.bluegecko.pixellm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LocalLlmService(
    private val context: Context,
    private val modelPath: String
) : Closeable {
    private val mutex = Mutex()
    private var llm: LlmInference? = null

    suspend fun load() = withContext(Dispatchers.Default) {
        mutex.withLock {
            if (llm != null) return@withLock

            val options = LlmInference.LlmInferenceOptions.builder().setModelPath(modelPath)
                .setMaxTokens(1024).setMaxTopK(40).build()

            llm = LlmInference.createFromOptions(context, options)
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

    override fun close() {
        llm?.close()
        llm = null
    }
}