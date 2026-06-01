package me.bluegecko.pixellm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import me.bluegecko.pixellm.ui.theme.PixeLLMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startForegroundService(Intent(this, ServerService::class.java))

        enableEdgeToEdge()
        setContent {
            PixeLLMTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun AppScreen(modifier: Modifier = Modifier) {
    val status by LoadStatus.status.collectAsState()

    Text(
        text = when (status) {
            LoadStatus.Status.LOADING -> "Loading model..."
            LoadStatus.Status.HEALTHY -> "Model loaded successfully!"
            LoadStatus.Status.FAILED -> "Failed to load model."
        },
        modifier = modifier
    )
}