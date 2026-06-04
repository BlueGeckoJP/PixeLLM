package me.bluegecko.pixellm

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.core.content.ContextCompat
import me.bluegecko.pixellm.ui.theme.PixeLLMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as PixeLLMApplication

        startForegroundService(Intent(this, ServerService::class.java))

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
}

@Composable
fun AppScreen(app: PixeLLMApplication, modifier: Modifier = Modifier) {
    val status by LoadStatus.status.collectAsState()
    val loadedModel by app.llmManager.loadedModel.collectAsState()

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
       Column {
           Text(
               text = when (status) {
                   LoadStatus.Status.UNLOADED -> "No model loaded."
                   LoadStatus.Status.LOADING -> "Loading model..."
                   LoadStatus.Status.HEALTHY -> "Model loaded successfully!"
                   LoadStatus.Status.FAILED -> "Failed to load model."
               },
               modifier = modifier
           )

           SingleFilePicker(app.applicationContext)

           Text(text = "Loaded model: ${loadedModel?.name ?: "None"}")
       }
    }
}

@Composable
fun SingleFilePicker(context: Context) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            Log.d("SingleFilePicker", "Selected file: $uri")

            val intent = Intent(context, ServerService::class.java).apply {
                action = "LOAD_MODEL"
                putExtra("MODEL_URI", uri.toString())
                putExtra("MODEL_NAME", getUriFileName(context, uri))
                data = uri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            ContextCompat.startForegroundService(context, intent)
        }
    }

    Button(onClick = {
        launcher.launch("*/*")
    }) {
        Text("Pick a model file")
    }
}

fun getUriFileName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) {
            cursor.getString(nameIndex)
        } else {
            null
        }
    }
}
