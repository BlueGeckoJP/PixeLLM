package me.bluegecko.pixellm.routes

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import me.bluegecko.pixellm.LlmManager

fun Route.modelsRoutes(llmManager: LlmManager) {
    get("/v1/models") {
        val loadedModel = llmManager.loadedModel.value
        val modelObj = if (loadedModel == null) {
            ""
        } else {
            """
            {
                "id": "${loadedModel.filename}",
                "object": "model",
                "created": 0,
                "owned_by": "local"
            }
        """.trimIndent()
        }

        call.respondText(
            text = """
                {
                    "object": "list",
                    "data": [
                        $modelObj
                    ]
                }
            """.trimIndent(),
            contentType = ContentType.Application.Json
        )
    }
}