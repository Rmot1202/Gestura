package com.example.gestura

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
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
    val sourceLang: String? = null, // null = autodetect
    val targetLang: String = "en",
    val isLoading: Boolean = false,
    val error: String? = null,
    val detectedLang: String? = null
)

// ---------- ViewModel ----------
class TranslationViewModel(app: Application) : AndroidViewModel(app) {
    var state by mutableStateOf(TranslateUiState())
        private set

    fun onInputChange(v: String) { state = state.copy(input = v) }
    fun onTargetChange(v: String) { state = state.copy(targetLang = v) }
    fun onSourceChange(v: String?) { state = state.copy(sourceLang = v) }
    fun clearError() { state = state.copy(error = null) }

    fun swapLanguages() {
        val newSource = state.targetLang
        val newTarget = state.sourceLang ?: "en"
        state = state.copy(sourceLang = newSource, targetLang = newTarget, output = "", detectedLang = null)
    }

    fun translate() {
        val text = state.input.trim()
        if (text.isEmpty()) {
            state = state.copy(error = "Please enter text to translate.")
            return
        }
        viewModelScope.launch {
            state = state.copy(isLoading = true, error = null, output = "", detectedLang = null)
            try {
                val src = state.sourceLang?.let { toLocaleCode(it) } // null => autodetect
                val tgt = toLocaleCode(state.targetLang)
                val options = TranslateOptions().apply {
                    setStyle(TranslationStyle.FLUID) // FAITHFUL | FLUID | CREATIVE
                    // setInstructions("Be concise and natural")
                }

                val res = withContext(Dispatchers.IO) {
                    if (src == null) laraClient.translate(text, null, tgt, options)
                    else laraClient.translate(text, src, tgt, options)
                }

                state = state.copy(
                    isLoading = false,
                    output = res.translation ?: "",
                    detectedLang = null // set if your SDK version exposes detection
                )
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Translation failed.")
            }
        }
    }
}

// ---------- Language options + locale mapping ----------
private val LanguageOptions = listOf(
    "auto" to "Auto-detect",
    "en" to "English",
    "es" to "Spanish",
    "fr" to "French",
    "de" to "German",
    "ja" to "Japanese",
    "zh" to "Chinese (Simplified)",
    "ko" to "Korean",
    "pt" to "Portuguese",
    "ru" to "Russian",
    "ar" to "Arabic",
    "hi" to "Hindi"
)

private fun toLocaleCode(code: String): String = when (code.lowercase()) {
    "en" -> "en-US"
    "es" -> "es-ES"
    "fr" -> "fr-FR"
    "de" -> "de-DE"
    "ja" -> "ja-JP"
    "zh" -> "zh-CN"
    "ko" -> "ko-KR"
    "pt" -> "pt-PT"   // or pt-BR
    "ru" -> "ru-RU"
    "ar" -> "ar-SA"
    "hi" -> "hi-IN"
    else -> "${code.lowercase()}-${code.uppercase()}"
}

// ---------- Screen ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(vm: TranslationViewModel) {
    val state = vm.state
    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    LaunchedEffect(Unit) { tts = TextToSpeech(ctx) { } }
    DisposableEffect(Unit) { onDispose { tts?.shutdown() } }

    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Translate", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = state.input,
            onValueChange = vm::onInputChange,
            label = { Text("Input text") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ⬇️ weight is applied at the CALL SITE (RowScope) — this fixes your error
            LanguageDropdown(
                label = "Source",
                selected = state.sourceLang ?: "auto",
                onSelected = { vm.onSourceChange(if (it == "auto") null else it) },
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = vm::swapLanguages) { Text("Swap") }
            LanguageDropdown(
                label = "Target",
                selected = state.targetLang,
                onSelected = vm::onTargetChange,
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = vm::translate,
            enabled = !state.isLoading,
            modifier = Modifier.align(Alignment.End)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Translating…")
            } else {
                Text("Translate")
            }
        }

        if (state.output.isNotEmpty() || state.detectedLang != null) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.detectedLang?.let {
                        AssistChip(onClick = {}, label = { Text("Detected: ${it.uppercase()}") })
                    }
                    Text(text = state.output.ifEmpty { "—" }, style = MaterialTheme.typography.bodyLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            clipboard.setText(AnnotatedString(state.output))
                        }) { Text("Copy") }
                        OutlinedButton(onClick = {
                            val lang = when (state.targetLang) {
                                "en" -> Locale.US
                                "es" -> Locale("es")
                                "fr" -> Locale.FRENCH
                                "de" -> Locale.GERMAN
                                "ja" -> Locale.JAPANESE
                                "zh" -> Locale.SIMPLIFIED_CHINESE
                                "ko" -> Locale.KOREAN
                                "pt" -> Locale("pt")
                                "ru" -> Locale("ru")
                                "ar" -> Locale("ar")
                                "hi" -> Locale("hi")
                                else -> Locale.getDefault()
                            }
                            tts?.language = lang
                            tts?.speak(state.output, TextToSpeech.QUEUE_FLUSH, null, "gestura-tts")
                        }) { Text("Speak") }
                    }
                }
            }
        }

        state.error?.let { err ->
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ---------- Dropdown (accepts modifier; NO weight inside) ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    label: String,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = displayNameFor(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = modifier.menuAnchor()   // use parent-provided modifier (may include weight)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LanguageOptions.forEach { (code, name) ->
                DropdownMenuItem(
                    text = { Text("$name (${code.uppercase()})") },
                    onClick = {
                        onSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun displayNameFor(code: String): String =
    LanguageOptions.firstOrNull { it.first == code }?.second ?: code.uppercase()

// ---------- Route used by TranslationFragment ----------
@Composable
fun TranslationRoute() {
    val ctx = LocalContext.current
    val vm = remember { TranslationViewModel(ctx.applicationContext as Application) }
    TranslationScreen(vm)
}
