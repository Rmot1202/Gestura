// data/ai/AslNlp.kt
package com.example.gestura.data.ai

object AslNlp {
    fun buildMessages(gloss: String): List<Message> {
        val system = """
            You are an ASL-to-English captioner. 
            INPUT is ASL gloss (UPPERCASE tokens, optional fingerspelling like F-S: H E L L O).
            TASK:
            - Convert gloss to fluent English.
            - Join finger-spelled letters into words.
            - Handle WH/How questions with “?” and natural word order.
            - Keep names from fingerspelling capitalized.
            - Keep it concise: 1 short sentence suitable as a subtitle.
            Examples:
            GLOSS: HELLO NAME FS: R A V E N
            EN: Hello, Raven.
            GLOSS: YOU HELP ME PLEASE
            EN: Could you help me, please?
            GLOSS: WHERE BATHROOM
            EN: Where is the bathroom?
        """.trimIndent()

        val user = "GLOSS: $gloss\nEN:"
        return listOf(
            Message("system", system),
            Message("user", user)
        )
    }
}
