package com.example.gestura.data.ai

object AslNlp {
    fun buildMessages(gloss: String): List<ChatMessage> {
        val system = ChatMessage(
            role = "system",
            content = "You convert ASL gloss into clear, grammatical English. Keep it concise."
        )
        val user = ChatMessage(
            role = "user",
            content = "Gloss: $gloss"
        )
        return listOf(system, user)
    }
}
