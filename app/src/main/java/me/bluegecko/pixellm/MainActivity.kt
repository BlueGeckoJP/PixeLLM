package me.bluegecko.pixellm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import me.bluegecko.pixellm.ui.theme.PixeLLMTheme

data class URIMetadata(
    val filename: String,
    val size: Long,
    val extension: String
)

class MainActivity : ComponentActivity() {
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        startServerService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PixeLLMApplication

        requestNotificationPermissionThenStartService()

        enableEdgeToEdge()
        setContent {
            PixeLLMTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppScreen(
                        app,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionThenStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startServerService()
        }
    }

    private fun startServerService() {
        ContextCompat.startForegroundService(this, Intent(this, ServerService::class.java))
    }
}

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

            SingleFilePicker(app.applicationContext, modifier = Modifier.padding(8.dp))

            Text(
                text = "Loaded model: ${loadedModel?.filename ?: "None"}",
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun SingleFilePicker(context: Context, modifier: Modifier = Modifier) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("SingleFilePicker", "Selected file: $uri")

            val uriMetadata = getUriMetadata(context, uri)

            if (uriMetadata == null) {
                Log.e("SingleFilePicker", "Failed to retrieve metadata for selected file: $uri")
                return@rememberLauncherForActivityResult
            }

            if (uriMetadata.extension != "litertlm") {
                Log.e(
                    "SingleFilePicker",
                    "Selected file does not have \".litertlm\" extension: ${uriMetadata.filename}"
                )
                return@rememberLauncherForActivityResult
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
    }

    Button(onClick = {
        launcher.launch("*/*")
    }, modifier = modifier) {
        Text("Pick a model file")
    }
}

fun getUriMetadata(context: Context, uri: Uri): URIMetadata? {
    return context.contentResolver.query(
        uri, arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE
        ), null, null, null
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it != -1 }
            ?: return@use null
        val sizeIndex =
            cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it != -1 } ?: return@use null

        if (cursor.moveToFirst()) {
            val rawName = cursor.getString(nameIndex)
            val name = rawName
                .replace("^[^a-zA-Z0-9]".toRegex(), "_")
                .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val size = cursor.getLong(sizeIndex)
            val extension = rawName.substringAfterLast('.', missingDelimiterValue = "")
                .takeIf { it.isNotBlank() }?.lowercase()
            URIMetadata(name, size, extension ?: "")
        } else {
            null
        }
    }
}
