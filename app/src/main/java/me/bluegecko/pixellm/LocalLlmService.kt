package me.bluegecko.pixellm

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.MessageCallback
import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class LocalLlmService(
    private val context: Context,
    private val modelPath: String
) : Closeable {
    private val mutex = Mutex()
    private lateinit var engine: Engine

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (::engine.isInitialized) return@withLock

            val preparedModelPath = prepareModel().absolutePath
            val engineConfig = EngineConfig(
                modelPath = preparedModelPath,
                backend = Backend.GPU()
            )

            engine = Engine(engineConfig)
            engine.initialize()
        }
    }


    suspend fun generateAsync(prompt: String, channel: Channel<String>): Unit =
        withContext(Dispatchers.Default) {
            mutex.withLock {
                check(::engine.isInitialized) {
                    "Engine not initialized. Call load() before generating."
                }

                val conversation = engine.createConversation()
                val done = CompletableDeferred<Unit>()

                conversation.sendMessageAsync(prompt, object : MessageCallback {
                        override fun onMessage(message: Message) {
                            Log.d("LLM", "Received message: ${message.contents}")
                            channel.trySend(message.contents.toString())
                        }

                        override fun onDone() {
                            Log.d("LLM", "Generation done")
                            channel.close()
                            done.complete(Unit)
                        }

                        override fun onError(throwable: Throwable) {
                            Log.e("LLM", "Generation error", throwable)
                            channel.close(throwable)
                            done.completeExceptionally(throwable)
                        }
                    })

                done.await()
                conversation.close()
                Log.i("LLM", "Conversation closed")
            }
        }

    override fun close() {
        if (::engine.isInitialized) engine.close()

    }

    private fun prepareModel(): File {
        Log.i("LLM", "Preparing model from path: $modelPath")

        val modelFile = File(modelPath)
        val modelDir = File(context.filesDir, "llm").apply { mkdirs() }
        val target = File(modelDir, modelFile.name.ifBlank { "model.litertlm" })

        if (target.exists() && (!modelFile.exists() || target.length() == modelFile.length())) {
            return target
        }

        require(modelFile.exists()) {
            "Model file does not exist: $modelPath"
        }

        val tmp = File(modelDir, "${target.name}.tmp")
        modelFile.inputStream().use { input ->
            tmp.outputStream().use { output ->
                input.copyTo(output, bufferSize = 1024 * 1024)
            }
        }

        if (target.exists() && !target.delete()) {
            error("Could not replace existing model file: ${target.absolutePath}")
        }
        check(tmp.renameTo(target)) {
            "Could not move model file to target location: ${target.absolutePath}"
        }

        Log.i("LLM", "Model prepared at: ${target.absolutePath}")
        return target
    }
}