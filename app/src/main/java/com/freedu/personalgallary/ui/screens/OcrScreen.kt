package com.freedu.personalgallary.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freedu.personalgallary.ui.viewmodel.GalleryViewModel
import kotlinx.coroutines.launch

/** OCR index: searchable text inside photos (receipts, signs, cards). */
@Composable
fun OcrScreen(
    indexedCount: Int,
    scan: suspend ((done: Int, total: Int) -> Unit) -> GalleryViewModel.AiScanResult
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0 to 1) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastFound by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Text search", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "$indexedCount photos indexed · search matches words inside images",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "The gallery search box also matches recognized text — try \"receipt\", a name from a sign, or any visible word. Everything is recognized on this device.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                busy = true
                error = null
                scope.launch {
                    when (val r = scan { d, t -> progress = d to t }) {
                        is GalleryViewModel.AiScanResult.Done -> lastFound = r.count
                        is GalleryViewModel.AiScanResult.Unavailable ->
                            error = "Text model unavailable — needs Google Play services (one-time download)."
                    }
                    busy = false
                }
            },
            enabled = !busy
        ) { Text(if (indexedCount == 0) "Index my photos" else "Index new photos") }
        if (busy) {
            Spacer(Modifier.height(12.dp))
            Text("Reading text… ${progress.first}/${progress.second}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)))
        }
        if (lastFound != null && !busy) {
            Spacer(Modifier.height(8.dp))
            Text("Found text in $lastFound photos.", style = MaterialTheme.typography.bodySmall)
        }
        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(80.dp))
    }
}
