package me.bluegecko.pixellm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LlmManager(private val context: Context) {
    data class ModelInfo(
        val name: String,
        val uri: String
    )

    private val mutex = Mutex()
    private val _loadedModel: MutableStateFlow<ModelInfo?> = MutableStateFlow(null)
    val loadedModel: StateFlow<ModelInfo?> = _loadedModel
    private var llmService: LocalLlmService? = null

    suspend fun loadModel(modelInfo: ModelInfo) {
        Log.i("LlmManager", "Loading model: ${modelInfo.name} from URI: ${modelInfo.uri}")
        LoadStatus.set(LoadStatus.Status.LOADING)

        mutex.withLock {
            if (loadedModel.value != null) {
                throw IllegalStateException("A model is already loaded. Unload it before loading a new one.")
            }

            val service = LocalLlmService(context, modelInfo.uri)
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
            LoadStatus.set(LoadStatus.Status.UNLOADED)
        }
    }

    suspend fun generateAsync(prompt: String, channel: Channel<String>) {
        val service = llmService
            ?: throw IllegalStateException("No model loaded. Load a model before generating.")
        service.generateAsync(prompt, channel)
    }

}