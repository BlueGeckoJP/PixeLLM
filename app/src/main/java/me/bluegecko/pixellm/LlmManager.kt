package me.bluegecko.pixellm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LlmManager(private val context: Context) {
    data class ModelInfo(
        val name: String,
        val uri: String
    )

    private val mutex = Mutex()
    private var loadedModel: ModelInfo? = null
    private var llmService: LocalLlmService? = null

    suspend fun loadModel(modelInfo: ModelInfo) {
        Log.i("LlmManager", "Loading model: ${modelInfo.name} from URI: ${modelInfo.uri}")
        LoadStatus.set(LoadStatus.Status.LOADING)

        mutex.withLock {
            if (loadedModel != null) {
                throw IllegalStateException("A model is already loaded. Unload it before loading a new one.")
            }

            val service = LocalLlmService(context, modelInfo.uri)
            service.load()

            loadedModel = modelInfo
            llmService = service
        }

    }

    suspend fun unloadModel() {
        mutex.withLock {
            llmService?.close()
            llmService = null
            loadedModel = null
            LoadStatus.set(LoadStatus.Status.LOADING)
        }
    }

    suspend fun generateAsync(prompt: String, channel: Channel<String>) {
        val service = llmService
            ?: throw IllegalStateException("No model loaded. Load a model before generating.")
        service.generateAsync(prompt, channel)
    }

    fun loadedModel(): ModelInfo? = loadedModel
}