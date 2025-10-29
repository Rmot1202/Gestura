package com.example.gestura.caption

import android.content.Context
import android.util.Log
import com.example.gestura.data.ai.AslNlp
import com.example.gestura.data.ai.ChatRequest
import com.example.gestura.data.ai.OpenAiService
import retrofit2.HttpException
import java.io.File
import java.io.IOException

object NlpCaptioner {
    private const val TAG = "NlpCaptioner"

    // simple memo to avoid re-calling OpenAI for the same gloss
    private var lastGloss: String? = null
    private var lastSentence: String? = null

    suspend fun glossToSentence(
        context: Context,                  // kept to match your call sites (unused)
        gloss: String,
        keyProvider: () -> String
    ): String {
        // return cached result if same gloss
        if (gloss == lastGloss && !lastSentence.isNullOrBlank()) return lastSentence!!

        val key = keyProvider().trim()
        if (key.isEmpty()) {
            Log.e(TAG, "OpenAI key missing")
            return "[unrecognized]"
        }

        return try {
            // IMPORTANT: pass a lambda, not the String
            val api = OpenAiService.create({ key })

            val resp = api.chat(
                ChatRequest(
                    model = "gpt-4o-mini",
                    messages = AslNlp.buildMessages(gloss),
                    temperature = 0.2
                )
            )
            val out = resp.choices.firstOrNull()?.message?.content?.trim().orEmpty()
            val sentence = if (out.isBlank()) "[unrecognized]" else out
            lastGloss = gloss
            lastSentence = sentence
            sentence
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP ${e.code()} calling OpenAI", e)
            "[unrecognized]"
        } catch (e: IOException) {
            Log.e(TAG, "Network error calling OpenAI", e)
            "[unrecognized]"
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error calling OpenAI", e)
            "[unrecognized]"
        }
    }

    fun singleLineToSrt(caption: String, durationSec: Double): String {
        fun ts(sec: Double): String {
            val ms = (sec * 1000).toInt()
            val h = ms / 3_600_000
            val m = (ms % 3_600_000) / 60_000
            val s = (ms % 60_000) / 1000
            val r = ms % 1000
            return "%02d:%02d:%02d,%03d".format(h, m, s, r)
        }
        return buildString {
            appendLine("1")
            appendLine("${ts(0.0)} --> ${ts(durationSec)}")
            appendLine(caption.ifBlank { "[unrecognized]" })
            appendLine()
        }
    }

    fun saveSrt(context: Context, srt: String): File =
        File.createTempFile("captions_", ".srt", context.cacheDir).apply { writeText(srt) }
}
