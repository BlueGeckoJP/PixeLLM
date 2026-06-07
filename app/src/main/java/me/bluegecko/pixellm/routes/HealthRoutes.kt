package me.bluegecko.pixellm.routes

import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import me.bluegecko.pixellm.LoadStatus

fun Route.healthRoutes() {
    get("/health") {
        val status = when (LoadStatus.status.value) {
            LoadStatus.Status.UNLOADED -> "UNLOADED"
            LoadStatus.Status.LOADING -> "LOADING"
            LoadStatus.Status.HEALTHY -> "HEALTHY"
            LoadStatus.Status.FAILED -> "FAILED"
        }
        call.respondText(status)
    }
}