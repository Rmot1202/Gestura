package com.example.gestura

import android.app.Application
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.translated.lara.Credentials
import com.translated.lara.translator.TranslateOptions
import com.translated.lara.translator.TranslationStyle
import com.translated.lara.translator.Translator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// ---------- Lara client ----------
private val laraClient: Translator by lazy {
    val creds = Credentials(
        BuildConfig.LARA_ACCESS_KEY_ID,
        BuildConfig.LARA_ACCESS_KEY_SECRET
    )
    Translator(creds)
}

// ---------- UI state ----------
data class TranslateUiState(
    val input: String = "",
    val output: String = "",
    val sourceLang: String = "en",
    val targetLang: String = "es",
    val isLoading: Boolean = false,
    val isListening: Boolean = false,
    val error: String? = null
)

// ---------- ViewModel ----------
class TranslationViewModel(app: Application) : AndroidViewModel(app) {
    var state by mutableStateOf(TranslateUiState())
        private set

    fun onInputChange(v: String) { state = state.copy(input = v) }
    fun onTargetChange(v: String) { state = state.copy(targetLang = v) }
    fun onSourceChange(v: String) { state = state.copy(sourceLang = v) }
    fun setListening(on: Boolean) { state = state.copy(isListening = on) }
    fun clearError() { state = state.copy(error = null) }

    fun swapLanguages() {
        state = state.copy(
            sourceLang = state.targetLang,
            targetLang = state.sourceLang,
            input = state.output.ifEmpty { state.input },
            output = state.input
        )
    }

    fun translate() {
        val text = state.input.trim()
        if (text.isEmpty()) {
            state = state.copy(error = "Please enter text to translate.")
            return
        }
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null, output = "")
            try {
                val src = toLocaleCode(state.sourceLang)
                val tgt = toLocaleCode(state.targetLang)
                val options = TranslateOptions().apply {
                    setStyle(TranslationStyle.FLUID)
                }

                val res = withContext(Dispatchers.IO) {
                    laraClient.translate(text, src, tgt, options)
                }

                state = state.copy(
                    isLoading = false,
                    output = res.translation.orEmpty()
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Translation failed.")
            }
        }
    }
}

// ---------- Language list (with flags) ----------
private data class Lang(val code: String, val name: String, val flag: String, val locale: String)
private val Languages = listOf(
    Lang("en", "English", "🇺🇸", "en-US"),
    Lang("es", "Spanish", "🇪🇸", "es-ES"),
    Lang("fr", "French", "🇫🇷", "fr-FR"),
    Lang("de", "German", "🇩🇪", "de-DE"),
    Lang("it", "Italian", "🇮🇹", "it-IT"),
    Lang("pt", "Portuguese", "🇵🇹", "pt-PT"),
    Lang("ja", "Japanese", "🇯🇵", "ja-JP"),
    Lang("zh", "Chinese", "🇨🇳", "zh-CN")
)

private fun toLocaleCode(code: String): String =
    Languages.firstOrNull { it.code == code }?.locale ?: "${code}-${code.uppercase()}"

// ---------- Screen ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(vm: TranslationViewModel) {
    val state = vm.state
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current

    // Text-to-speech
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    LaunchedEffect(Unit) { tts = TextToSpeech(ctx) { } }
    DisposableEffect(Unit) { onDispose { tts?.shutdown() } }

    // Voice input via Recognizer intent
    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!text.isNullOrBlank()) vm.onInputChange(text)
        }
        vm.setListening(false)
    }
    fun startVoiceInput(lang: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, toLocaleCode(lang))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now…")
        }
        vm.setListening(true)
        voiceLauncher.launch(intent)
    }

    // Simple shadcn-style layout: header, cards, CTA, result, phrases
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(bottom = 16.dp)
    ) {
        // Header
        Column(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text("Language Translation", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Translate between languages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Content
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language selection card
            Card {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LanguageSelect(
                            label = null,
                            value = state.sourceLang,
                            onChange = vm::onSourceChange,
                            modifier = Modifier.weight(1f)
                        )

                        FilledTonalIconButton(onClick = vm::swapLanguages) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Swap")
                        }

                        LanguageSelect(
                            label = null,
                            value = state.targetLang,
                            onChange = vm::onTargetChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Source text card
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Type or speak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.input.isNotBlank()) {
                            TextButton(onClick = {
                                val loc = Languages.firstOrNull { it.code == state.sourceLang }?.let {
                                    when (it.code) {
                                        "en" -> Locale.US
                                        "es" -> Locale("es")
                                        "fr" -> Locale.FRENCH
                                        "de" -> Locale.GERMAN
                                        "it" -> Locale.ITALIAN
                                        "pt" -> Locale("pt")
                                        "ja" -> Locale.JAPANESE
                                        "zh" -> Locale.SIMPLIFIED_CHINESE
                                        else -> Locale.getDefault()
                                    }
                                } ?: Locale.getDefault()
                                tts?.language = loc
                                tts?.speak(state.input, TextToSpeech.QUEUE_FLUSH, null, "tts-src")
                            }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Play")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.input,
                        onValueChange = vm::onInputChange,
                        placeholder = { Text("Enter text…") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { startVoiceInput(state.sourceLang) },
                            enabled = !state.isListening,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Outlined.Mic,
                                contentDescription = null,
                                tint = if (state.isListening) MaterialTheme.colorScheme.error else LocalContentColor.current
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.isListening) "Listening…" else "Voice")
                        }
                        if (state.input.isNotBlank()) {
                            FilledTonalIconButton(onClick = { vm.onInputChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    }
                }
            }

            // Translate button
            Button(
                onClick = vm::translate,
                enabled = state.input.isNotBlank() && !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Translating…")
                } else {
                    Text("Translate")
                }
            }

            // Result card
            if (state.output.isNotBlank()) {
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Translation", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = {
                                val loc = Languages.firstOrNull { it.code == state.targetLang }?.let {
                                    when (it.code) {
                                        "en" -> Locale.US
                                        "es" -> Locale("es")
                                        "fr" -> Locale.FRENCH
                                        "de" -> Locale.GERMAN
                                        "it" -> Locale.ITALIAN
                                        "pt" -> Locale("pt")
                                        "ja" -> Locale.JAPANESE
                                        "zh" -> Locale.SIMPLIFIED_CHINESE
                                        else -> Locale.getDefault()
                                    }
                                } ?: Locale.getDefault()
                                tts?.language = loc
                                tts?.speak(state.output, TextToSpeech.QUEUE_FLUSH, null, "tts-out")
                            }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Play")
                            }
                        }

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                                .padding(14.dp)
                        ) {
                            Text(state.output, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                clipboard.setText(AnnotatedString(state.output))
                            }) { Text("Copy") }
                        }
                    }
                }
            }

        }

        // Error line (if any)
        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

// ---------- Language select (flag + name) ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelect(
    label: String? = null,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        val display = Languages.firstOrNull { it.code == value }?.let { "${it.flag} ${it.name}" } ?: value
        OutlinedTextField(
            value = display,
            onValueChange = {},
            readOnly = true,
            label = label?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Languages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text("${lang.flag} ${lang.name}") },
                    onClick = {
                        onChange(lang.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ---------- Route used by TranslationFragment ----------
@Composable
fun TranslationRoute() {
    val ctx = LocalContext.current
    val vm = remember { TranslationViewModel(ctx.applicationContext as Application) }
    TranslationScreen(vm)
}
