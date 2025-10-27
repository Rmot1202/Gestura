// caption/NlpCaptioner.kt
package com.example.gestura.caption

import android.content.Context
import com.example.gestura.data.ai.AslNlp
import com.example.gestura.data.ai.ChatRequest
import com.example.gestura.data.ai.OpenAiService
import java.io.File

object NlpCaptioner {

    suspend fun glossToSentence(
        context: Context,
        gloss: String,
        keyProvider: () -> String
    ): String {
        val api = OpenAiService.create(keyProvider)
        val req = ChatRequest(
            model = "gpt-4o-mini",
            messages = AslNlp.buildMessages(gloss),
            temperature = 0.2
        )
        val resp = api.chat(req)
        return resp.choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }

    fun singleLineToSrt(caption: String, durationSec: Double): String {
        fun ts(sec: Double): String {
            val ms = (sec * 1000).toInt()
            val h = ms / 3600000
            val m = (ms % 3600000) / 60000
            val s = (ms % 60000) / 1000
            val r = ms % 1000
            return "%02d:%02d:%02d,%03d".format(h, m, s, r)
        }
        // whole-video single caption; or split by punctuation later
        return buildString {
            appendLine("1")
            appendLine("${ts(0.0)} --> ${ts(durationSec)}")
            appendLine(caption.ifBlank { "[inaudible]" })
            appendLine()
        }
    }

    fun saveSrt(context: Context, srt: String): File =
        File.createTempFile("captions_", ".srt", context.cacheDir).apply { writeText(srt) }
}
