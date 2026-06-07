package me.bluegecko.pixellm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking

class ServerService : Service() {
    private lateinit var llmManager: LlmManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loadDeferred: Deferred<Unit>? = null
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
        null

    override fun onCreate() {
        super.onCreate()

        this.startForeground()

        val app = application as PixeLLMApplication
        llmManager = app.llmManager

        server = KtorServer.start(
            port = 8080,
            context = this,
            getLoadDeferred = { loadDeferred }
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "LOAD_MODEL" -> {
                val modelFilename =
                    intent.getStringExtra("MODEL_FILENAME") ?: "unknown_model.litertlm"
                val modelSize = intent.getLongExtra("MODEL_SIZE", -1L)
                val modelUri = intent.getStringExtra("MODEL_URI")
                if (modelUri != null) {
                    loadDeferred = serviceScope.async {
                        llmManager.loadModel(
                            LlmManager.ModelInfo(
                                filename = modelFilename,
                                uri = modelUri,
                                size = modelSize
                            )
                        )
                    }

                    loadDeferred?.invokeOnCompletion { cause ->
                        val status = when {
                            cause == null -> LoadStatus.Status.HEALTHY
                            cause is LlmManager.ModelAlreadyLoadedException -> {
                                Log.w("ServerService", "Model already loaded", cause)
                                loadDeferred = null
                                LoadStatus.Status.HEALTHY
                            }

                            else -> {
                                Log.e("ServerService", "Failed to load model", cause)
                                LoadStatus.Status.FAILED
                            }
                        }

                        LoadStatus.set(status)
                    }
                } else {
                    Log.e("ServerService", "MODEL_URI extra is missing in LOAD_MODEL intent")
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

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("PixeLLM server running")
            .setContentText("Tap to open app")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .build()
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
