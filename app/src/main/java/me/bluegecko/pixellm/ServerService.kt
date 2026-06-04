package me.bluegecko.pixellm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class ServerService : Service() {
    private lateinit var llmManager: LlmManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loadDeferred: Deferred<Unit>? = null
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
        null
    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate() {
        super.onCreate()

        this.startForeground()

        llmManager = LlmManager(this)

        server = embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
            routing {
                get("/health") {
                    val status = when (LoadStatus.status.value) {
                        LoadStatus.Status.LOADING -> "LOADING"
                        LoadStatus.Status.HEALTHY -> "HEALTHY"
                        LoadStatus.Status.FAILED -> "FAILED"
                    }

                    call.respondText(status)
                }
                post("/v1/chat/completions") {
                    try {
                        loadDeferred?.await()

                        val rawRequest = call.receiveText()
                        val request = json.decodeFromString<ChatCompletions>(rawRequest)
                        val messages =
                            request.messages.joinToString(separator = "\n") { "${it.role}: ${it.content.toString()}" }
                        Log.d("ServerService", "Received raw request: $rawRequest")
                        Log.d("ServerService", "Parsed object: $request")
                        Log.d("ServerService", "Received prompt: $messages")
                        val channel = Channel<String>(Channel.UNLIMITED)

                        call.respondTextWriter(ContentType.Text.EventStream) {
                            val generationJob = launch {
                                llmManager.generateAsync(messages, channel)
                            }

                            try {
                                for (chunk in channel) {
                                    write(
                                        // @formatter:off
                                        """data: {"choices":[{"delta":{"content":${Json.encodeToString(chunk)}}}]}""" + "\n\n"
                                        // @formatter:on
                                    )

                                    flush()
                                }

                                write("data: [DONE]\n\n")
                                flush()
                            } finally {
                                generationJob.cancel()
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e("ServerService", "Error handling request", e)
                        call.respondText(
                            text = "Error: ${e.message}",
                            status = HttpStatusCode.ServiceUnavailable
                        )
                    }
                }
                get("/v1/models") {
                    val currentModel = llmManager.loadedModel()
                    val modelObj = if (currentModel == null) {
                        "{}"
                    } else {
                        """
                                    {
                                        "id": "${currentModel.name}",
                                        "object": "model",
                                        "created": 0,
                                        "owned_by": "local"
                                    }
                        """
                    }

                    call.respondText(
                        text = """
                            {
                                "object": "list",
                                "data": [
                                    $modelObj
                                ]
                            }
                        """,
                        contentType = ContentType.Application.Json
                    )
                }
            }
        }.start(wait = false)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "LOAD_MODEL" -> {
                val modelName = intent.getStringExtra("MODEL_NAME") ?: "unknown_model"
                val modelUri = intent.getStringExtra("MODEL_URI")
                if (modelUri != null) {
                    loadDeferred = serviceScope.async {
                        llmManager.loadModel(LlmManager.ModelInfo(name = modelName, uri = modelUri))
                    }

                    loadDeferred?.invokeOnCompletion { cause ->
                        val status = when {
                            cause == null -> LoadStatus.Status.HEALTHY
                            else -> {
                                Log.e("ServerService", "Failed to load model", cause)
                                LoadStatus.Status.FAILED
                            }
                        }

                        LoadStatus.set(status)
                    }
                } else {
                    Log.e("ServerService", "MODEL_PATH extra is missing in LOAD_MODEL intent")
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        server?.stop(1000, 2000)
        server = null
        runBlocking {
            llmManager.unloadModel()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun startForeground() {
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "PixeLLM Server Service",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            notificationChannel
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher).setContentTitle("PixeLLM server running")
            .setContentText("Tap to open app").setOngoing(true).build()
        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "pixellm_server_channel"
        private const val NOTIFICATION_ID = 1
    }
}
