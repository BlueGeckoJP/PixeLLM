package me.bluegecko.pixellm.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import me.bluegecko.pixellm.LoadStatus
import me.bluegecko.pixellm.PixeLLMApplication
import me.bluegecko.pixellm.ServerService
import me.bluegecko.pixellm.util.readUriMetadata

@Composable
fun AppScreen(app: PixeLLMApplication, modifier: Modifier = Modifier) {
    val status by LoadStatus.status.collectAsState()
    val loadedModel by app.llmManager.loadedModel.collectAsState()

    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        Column {
            Text(
                text = when (status) {
                    LoadStatus.Status.UNLOADED -> "No model loaded."
                    LoadStatus.Status.LOADING -> "Loading model..."
                    LoadStatus.Status.HEALTHY -> "Model loaded successfully!"
                    LoadStatus.Status.FAILED -> "Failed to load model."
                },
                modifier = Modifier.padding(8.dp)
            )

            ModelFilePicker(modifier = Modifier.padding(8.dp)) { uri ->
                Log.d("ModelFilePicker", "Selected file: $uri")

                val context = app.applicationContext
                loadModelFromUri(context, uri)
            }

            Text(
                text = "Loaded model: ${loadedModel?.filename ?: "None"}",
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

private fun loadModelFromUri(context: Context, uri: Uri) {
    val uriMetadata = readUriMetadata(context, uri)

    if (uriMetadata == null) {
        Log.e("ModelFilePicker", "Failed to retrieve metadata for selected file: $uri")
        return
    }

    if (uriMetadata.extension != "litertlm") {
        Log.e(
            "ModelFilePicker",
            "Selected file does not have \".litertlm\" extension: ${uriMetadata.filename}"
        )
        return
    }

    val intent = Intent(context, ServerService::class.java).apply {
        action = "LOAD_MODEL"
        putExtra("MODEL_URI", uri.toString())
        putExtra("MODEL_FILENAME", uriMetadata.filename)
        putExtra("MODEL_SIZE", uriMetadata.size)
        data = uri
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    ContextCompat.startForegroundService(context, intent)
}