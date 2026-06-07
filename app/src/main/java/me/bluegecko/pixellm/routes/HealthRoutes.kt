package me.bluegecko.pixellm.routes

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import me.bluegecko.pixellm.LoadStatusStore
import me.bluegecko.pixellm.model.LoadStatus

fun Route.healthRoutes() {
    get("/health") {
        val status = when (LoadStatusStore.status.value) {
           LoadStatus.UNLOADED -> "UNLOADED"
            LoadStatus.LOADING -> "LOADING"
            LoadStatus.HEALTHY -> "HEALTHY"
            LoadStatus.FAILED -> "FAILED"
        }
        call.respondText(status)
    }
}