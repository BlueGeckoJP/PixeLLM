package me.bluegecko.pixellm

import android.content.Context
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LlmManager(private val context: Context) {
    data class ModelInfo(
        val name: String,
        val path: String
    )

    private val mutex = Mutex()
    private val _allModels = MutableStateFlow(listOf<ModelInfo>())
    val allModels: StateFlow<List<ModelInfo>> = _allModels

    private var currentModel: ModelInfo? = null
    private var llmService: LocalLlmService? = null

    suspend fun loadModel(modelInfo: ModelInfo) {
        if (!allModels.value.contains(modelInfo)) {
            throw IllegalArgumentException("Model ${modelInfo.name} not found in available models.")
        }

        LoadStatus.set(LoadStatus.Status.LOADING)

        mutex.withLock {
            if (currentModel != null) {
                throw IllegalStateException("A model is already loaded. Unload it before loading a new one.")
            }

            val service = LocalLlmService(context, modelInfo.path)
            service.load()

            currentModel = modelInfo
            llmService = service
        }

    }

    suspend fun unloadModel() {
        mutex.withLock {
            llmService?.close()
            llmService = null
            currentModel = null
            LoadStatus.set(LoadStatus.Status.LOADING)
        }
    }

    suspend fun generateAsync(prompt: String, channel: Channel<String>) {
        val service = llmService
            ?: throw IllegalStateException("No model loaded. Load a model before generating.")
        service.generateAsync(prompt, channel)
    }

    fun addModel(modelInfo: ModelInfo) {
        _allModels.value += modelInfo
    }

    fun removeModel(modelInfo: ModelInfo) {
        _allModels.value -= modelInfo
    }

    fun currentModel(): ModelInfo? = currentModel
}