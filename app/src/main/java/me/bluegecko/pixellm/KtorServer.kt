package me.bluegecko.pixellm

import android.content.Context
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.Deferred
import me.bluegecko.pixellm.plugins.configureRouting

object KtorServer {
    fun start(
        port: Int,
        context: Context,
        getLoadDeferred: () -> Deferred<Unit>?
    ): EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration> {
        return embeddedServer(
            factory = CIO,
            port,
            host = "0.0.0.0"
        ) {
            module(context, getLoadDeferred)
        }.start(wait = false)
    }
}

fun Application.module(context: Context, getLoadDeferred: () -> Deferred<Unit>?) {
    val app = context.applicationContext as PixeLLMApplication
    configureRouting(app.llmManager, getLoadDeferred)
}
