package me.bluegecko.pixellm.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ModelFilePicker(modifier: Modifier = Modifier, onFilePicked: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            Log.i("ModelFilePicker", "File picked: $uri")
            onFilePicked(uri)
        } else {
            Log.i("ModelFilePicker", "No file picked")
        }
    }

    Button(
        onClick = {
            launcher.launch("*/*")
        },
        modifier = modifier,
    ) {
        Text("Pick a model file")
    }
}