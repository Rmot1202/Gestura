// ui/OnDeviceCaptionScreen.kt
package com.example.gestura.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gestura.caption.Captioner
import com.example.gestura.caption.Segment
import com.example.gestura.caption.NlpCaptioner
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun OnDeviceCaptionScreen(
    // Provide a way to get your stored API key (EncryptedSharedPreferences recommended)
    keyProvider: () -> String = { Settings.openAiKey } // <-- replace with your actual getter
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var srtFile by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    var glossText by remember { mutableStateOf<String?>(null) }
    var finalCaption by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            errorMsg = null
            finalCaption = null
            glossText = null
            // FEAT_DIM must match your training (e.g., hands(126) or holistic total)
            val FEAT_DIM = 126

            scope.launch {
                try {
                    // 1) On-device ASL recognition → segments (+ old SRT we will replace)
                    val (segments, _) = Captioner.run(context, uri, FEAT_DIM)

                    // 2) Build GLOSS from merged segments
                    val gloss = segments.joinToString(" ") { it.token }
                    glossText = gloss

                    // 3) Duration (use last segment end as a safe estimate)
                    val duration = segments.maxOfOrNull(Segment::end) ?: 0.0

                    // 4) Call OpenAI to turn gloss → natural English
                    val sentence = NlpCaptioner.glossToSentence(
                        context = context,
                        gloss = gloss,
                        keyProvider = keyProvider
                    )
                    finalCaption = sentence.ifBlank { "[unrecognized]" }

                    // 5) Build single-line SRT that spans the whole video duration
                    val srt = NlpCaptioner.singleLineToSrt(finalCaption!!, duration.coerceAtLeast(0.5))

                    // 6) Save and display
                    val tmp = NlpCaptioner.saveSrt(context, srt)
                    srtFile = tmp
                    videoUri = uri
                } catch (t: Throwable) {
                    errorMsg = t.message ?: "Captioning failed."
                } finally {
                    busy = false
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { picker.launch(arrayOf("video/*")) }, enabled = !busy) {
                Text(if (busy) "Processing..." else "Pick a video")
            }
            if (videoUri != null && srtFile != null) {
                OutlinedButton(onClick = { videoUri = null; srtFile = null; finalCaption = null; glossText = null }) {
                    Text("Clear")
                }
            }
        }

        if (errorMsg != null) {
            Spacer(Modifier.height(12.dp))
            Text("Error: $errorMsg", color = MaterialTheme.colorScheme.error)
        }

        if (glossText != null || finalCaption != null) {
            Spacer(Modifier.height(12.dp))
            if (glossText != null) {
                Text("GLOSS: ${glossText}", style = MaterialTheme.typography.bodyMedium)
            }
            if (finalCaption != null) {
                Text("Caption: ${finalCaption}", style = MaterialTheme.typography.titleMedium)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (videoUri != null && srtFile != null) {
            PlayerWithSubs(videoUri!!, Uri.fromFile(srtFile!!), Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlayerWithSubs(video: Uri, srt: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val mediaItem = MediaItem.Builder()
        .setUri(video)
        .setSubtitleConfigurations(
            listOf(
                SubtitleConfiguration.Builder(srt)
                    .setMimeType(MimeTypes.TEXT_SRT)
                    .setLanguage("en")
                    .build()
            )
        ).build()

    LaunchedEffect(video, srt) {
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = false
    }

    AndroidView(
        factory = { StyledPlayerView(it).apply { this.player = player } },
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Replace this with your actual EncryptedSharedPreferences-backed settings accessor.
 */
private object Settings {
    // e.g., read from EncryptedSharedPreferences
    val openAiKey: String
        get() = "" // TODO: provide user-entered key here
}
