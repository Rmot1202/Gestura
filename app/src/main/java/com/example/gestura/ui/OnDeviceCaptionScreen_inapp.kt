package com.example.gestura.ui

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gestura.BuildConfig
import com.example.gestura.caption.Captioner
import com.example.gestura.caption.NlpCaptioner
import com.example.gestura.caption.Segment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.MediaItem.SubtitleConfiguration
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnDeviceCaptionScreen_inapp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var srtFile by remember { mutableStateOf<File?>(null) }
    var busy by remember { mutableStateOf(false) }
    var glossText by remember { mutableStateOf<String?>(null) }
    var finalCaption by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var segments by remember { mutableStateOf<List<Segment>>(emptyList()) }
    var durationSec by remember { mutableStateOf(0.0) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentOverlay by remember { mutableStateOf<String?>(null) }
    var currentConfidence by remember { mutableStateOf<Int?>(null) }

    // Simple history of saved sentences
    var history by remember { mutableStateOf(listOf<String>()) }

    // TTS instance held in state
    var ttsInstance by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        // Declare a mutable variable to hold the TextToSpeech instance.
        // This ensures the variable 'tts' is resolved at compile time.
        var tts: TextToSpeech? = null

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // At this point, 'tts' has been assigned the TextToSpeech instance.
                // We can use the non-null assertion operator '!!' here because
                // onInit is called after successful construction.
                tts!!.language = Locale.US
                ttsInstance = tts // Assign the configured TTS to the Composable's state
            } else {
                ttsInstance = null
            }
        }
        onDispose {
            // Use safe call '?' because 'tts' could potentially still be null
            // if the TextToSpeech constructor itself failed before assignment.
            tts?.stop()
            tts?.shutdown()
        }
    }

    // SAF picker
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            errorMsg = null
            finalCaption = null
            glossText = null
            segments = emptyList()
            srtFile = null
            videoUri = null
            currentOverlay = null
            currentConfidence = null
            isPlaying = false

            // Persist permission
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            // Must match your model’s input feature dimension
            val FEAT_DIM = 126 // hands-only example: 2*21*3

            scope.launch {
                try {
                    val apiKey = BuildConfig.OPENAI_API_KEY
                    require(apiKey.isNotBlank()) {
                        "Missing OPENAI_API_KEY. Add it to local.properties and Gradle Sync."
                    }

                    // 1) On-device pipeline → segments + duration
                    val (segs, dur) = Captioner.run(context, uri, FEAT_DIM)
                    segments = segs
                    durationSec = dur

                    // 2) Gloss from tokens
                    val gloss = segs.joinToString(" ") { it.token }
                    glossText = gloss

                    // 3) NLP: gloss → natural sentence
                    val sentence = NlpCaptioner.glossToSentence(
                        context = context,
                        gloss = gloss,
                        keyProvider = { apiKey }
                    ).ifBlank { "[unrecognized]" }
                    finalCaption = sentence

                    // 4) Build SRT for full duration (fallback to last segment end)
                    val total = if (dur > 0.1) dur else (segs.maxOfOrNull(Segment::end) ?: 5.0)
                    val srt = NlpCaptioner.singleLineToSrt(sentence, total)

                    // 5) Save SRT and set URI
                    srtFile = NlpCaptioner.saveSrt(context, srt)
                    videoUri = uri
                } catch (t: Throwable) {
                    errorMsg = t.message ?: "Captioning failed."
                } finally {
                    busy = false
                }
            }
        }
    }

    // UI
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(tonalElevation = 1.dp, shadowElevation = 0.dp) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("ASL Translation", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Upload video to translate ASL signs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Scrollable content
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Video card (aspect 3/4, rounded)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF111827))
            ) {
                if (videoUri != null && srtFile != null) {
                    PlayerWithOverlay(
                        video = videoUri!!,
                        srt = Uri.fromFile(srtFile!!),
                        segments = segments,
                        desiredPlaying = isPlaying,
                        onPlayingChanged = { isPlaying = it },
                        onOverlayChanged = { text, conf ->
                            currentOverlay = text
                            currentConfidence = conf
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Clear button (top-right)
                    TextButton(
                        onClick = {
                            videoUri = null
                            srtFile = null
                            finalCaption = null
                            glossText = null
                            segments = emptyList()
                            currentOverlay = null
                            currentConfidence = null
                            errorMsg = null
                            isPlaying = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) { Text("✕") }

                    // Live overlay like the React version (bottom gradient)
                    if (!currentOverlay.isNullOrBlank() && isPlaying) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                currentConfidence?.let { conf ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("$conf% confident") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = Color.White.copy(alpha = 0.2f),
                                            labelColor = Color.White
                                        )
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                Text(
                                    currentOverlay!!,
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    // Empty state
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "No video uploaded",
                            color = Color(0xFF9CA3AF),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { picker.launch(arrayOf("video/*")) }, enabled = !busy) {
                            Text(if (busy) "Processing..." else "Upload Video")
                        }
                    }
                }
            }

            // Controls (Upload + Play/Pause)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (videoUri != null && srtFile != null) {
                    Button(onClick = { isPlaying = !isPlaying }) {
                        Text(if (isPlaying) "Pause" else "Play")
                    }
                }
            }

            // Current translation card (like the React card)
            if (!finalCaption.isNullOrBlank() && !isPlaying) {
                Card {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                "Current Translation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            currentConfidence?.let { conf ->
                                AssistChip(onClick = {}, label = { Text("$conf% confident") })
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                finalCaption!!,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = {
                                val tts = ttsInstance
                                val text = finalCaption
                                if (tts != null && !text.isNullOrBlank()) {
                                    tts.stop()
                                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts-caption")
                                }
                            }) { Text("Speak") }
                            OutlinedButton(onClick = {
                                if (!finalCaption.isNullOrBlank() && !history.contains(finalCaption!!)) {
                                    history = listOf(finalCaption!!) + history
                                }
                            }) { Text("Save") }
                        }
                    }
                }
            }

            // Detected signs (from segments, most recent first)
            if (segments.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Detected Signs", style = MaterialTheme.typography.titleMedium)
                    segments.asReversed().forEach { seg ->
                        val confText = segConfidence(seg)?.let { "$it%" }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(seg.token, style = MaterialTheme.typography.bodyLarge)
                                confText?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                            }
                        }
                    }
                }
            }

            // Saved translations (history)
            if (history.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Saved Translations", style = MaterialTheme.typography.titleMedium)
                    history.take(5).forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                item,
                                Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            if (!errorMsg.isNullOrBlank()) {
                Text("Error: $errorMsg", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/**
 * Player with SRT subtitles + live overlay text.
 * - Prefers current subtitle cue text;
 * - Falls back to latest finished Segment token;
 * - Reports "confidence" if available on Segment (optional).
 */
@Composable
private fun PlayerWithOverlay(
    video: Uri,
    srt: Uri,
    segments: List<Segment>,
    desiredPlaying: Boolean,
    onPlayingChanged: (Boolean) -> Unit,
    onOverlayChanged: (text: String?, confidence: Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val mediaItem = MediaItem.Builder()
        .setUri(video)
        .setSubtitleConfigurations(
            listOf(
                SubtitleConfiguration.Builder(srt)
                    .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .setLanguage("en")
                    .build()
            )
        ).build()

    // Prepare when media changes
    LaunchedEffect(video, srt) {
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = false
        onPlayingChanged(false)
    }

    // Apply external play/pause intent
    LaunchedEffect(desiredPlaying) {
        player.playWhenReady = desiredPlaying
    }

    // Observe playback, update overlay
    LaunchedEffect(player, segments) {
        while (true) {
            onPlayingChanged(player.isPlaying)

            val cueText = player.currentCues.cues.firstOrNull()?.text?.toString()
            if (!cueText.isNullOrBlank()) {
                onOverlayChanged(cueText, null)
            } else {
                val tSec = player.currentPosition / 1000.0
                val seg = segments.filter { it.end <= tSec }.maxByOrNull { it.end }
                if (seg != null) {
                    onOverlayChanged(seg.token, segConfidence(seg))
                } else {
                    onOverlayChanged(null, null)
                }
            }
            delay(150)
        }
    }

    AndroidView(
        factory = { StyledPlayerView(it).apply { this.player = player } },
        modifier = modifier
    )
}

/**
 * If your Segment has a confidence/score field, expose it here.
 * Return Int percent (0–100) or null if not available.
 *
 * Example (uncomment and adjust if your Segment looks like: data class Segment(..., score: Float?)):
 *   return segment.score?.let { (it * 100f).toInt().coerceIn(0, 100) }
 */
private fun segConfidence(segment: Segment): Int? {
    // TODO: connect to your real confidence if available on Segment
    return null
}