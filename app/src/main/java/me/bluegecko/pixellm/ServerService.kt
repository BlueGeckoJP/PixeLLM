package me.bluegecko.pixellm

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

class ServerService : Service() {
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
        null

    override fun onCreate() {
        super.onCreate()

        server = embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
            routing {
                get("/health") {
                    call.respondText("Healthy")
                }
            }
        }.start(wait = false)
    }

    override fun onDestroy() {
        server?.stop(1000, 2000)
        server = null
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null
}
