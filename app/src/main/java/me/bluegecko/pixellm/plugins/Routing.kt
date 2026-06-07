package me.bluegecko.pixellm.plugins

import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import kotlinx.coroutines.Deferred
import me.bluegecko.pixellm.LlmManager
import me.bluegecko.pixellm.routes.chatRoutes
import me.bluegecko.pixellm.routes.healthRoutes
import me.bluegecko.pixellm.routes.modelsRoutes

fun Application.configureRouting(llmManager: LlmManager, getLoadDeferred: () -> Deferred<Unit>?) {
    routing {
        healthRoutes()
        chatRoutes(llmManager, getLoadDeferred)
        modelsRoutes(llmManager)
    }
}