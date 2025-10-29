package com.example.gestura.data.ai

import com.squareup.moshi.JsonClass

// ---- request DTOs ----
@JsonClass(generateAdapter = true)
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val role: String,      // "system" | "user" | "assistant"
    val content: String
)

// ---- response DTOs ----
@JsonClass(generateAdapter = true)
data class ChatResponse(
    val id: String? = null,
    val choices: List<ChatChoice> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
    val index: Int? = null,
    val message: ChatMessage? = null,
    val finish_reason: String? = null
)
