package me.bluegecko.pixellm

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel

class ServerService : Service() {
    private lateinit var llmService: LocalLlmService
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var loadDeferred: Deferred<Unit>
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
        null

    override fun onCreate() {
        super.onCreate()

        llmService = LocalLlmService(this, "/data/local/tmp/llm/model.task")

        loadDeferred = serviceScope.async {
            llmService.load()
        }

        server = embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
            routing {
                get("/health") {
                    val status = when {
                        !loadDeferred.isCompleted -> "Loading"
                        loadDeferred.isCancelled -> "Failed"
                        else -> "Healthy"
                    }

                    call.respondText(status)
                }
                post("/v1/chat/completions") {
                    try {
                        loadDeferred.await()

                        val prompt = call.receiveText()
                        val response = llmService.generate(prompt)
                        call.respondText(response)
                    } catch (e: Throwable) {
                        call.respondText(
                            text = "Error: ${e.message}",
                            status = HttpStatusCode.ServiceUnavailable
                        )
                    }
                }
            }
        }.start(wait = false)
    }

    override fun onDestroy() {
        server?.stop(1000, 2000)
        server = null
        serviceScope.cancel()
        llmService.close()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}
