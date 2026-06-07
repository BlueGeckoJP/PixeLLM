package me.bluegecko.pixellm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.bluegecko.pixellm.model.LoadStatus
import me.bluegecko.pixellm.model.ModelInfo

class LlmManager(private val context: Context) {
    class ModelAlreadyLoadedException(message: String) : Exception(message)

    private val mutex = Mutex()
    private val _loadedModel: MutableStateFlow<ModelInfo?> = MutableStateFlow(null)
    val loadedModel: StateFlow<ModelInfo?> = _loadedModel
    private var llmService: LocalLlmService? = null

    suspend fun loadModel(modelInfo: ModelInfo) {
        mutex.withLock {
            Log.i(
                "LlmManager",
                "Loading model: ${modelInfo.filename} from URI: ${modelInfo.uri}, size: ${modelInfo.size} bytes"
            )

            if (loadedModel.value != null) {
                throw ModelAlreadyLoadedException("A model is already loaded. Unload it before loading a new one.")
            }

            LoadStatusStore.set(LoadStatus.LOADING)

            val service = LocalLlmService(context, modelInfo)
            service.load()

            _loadedModel.value = modelInfo
            llmService = service
        }

    }

    suspend fun unloadModel() {
        mutex.withLock {
            llmService?.close()
            llmService = null
            _loadedModel.value = null
            LoadStatusStore.set(LoadStatus.UNLOADED)
        }
    }

    suspend fun generateAsync(prompt: String, channel: Channel<String>) {
        val service = llmService
            ?: throw IllegalStateException("No model loaded. Load a model before generating.")
        service.generateAsync(prompt, channel)
    }

    suspend fun generate(prompt: String): String {
        val service = llmService
            ?: throw IllegalStateException("No model loaded. Load a model before generating.")
        return service.generate(prompt)
    }
}