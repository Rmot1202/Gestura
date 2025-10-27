// data/ai/OpenAiService.kt
package com.example.gestura.data.ai

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// ----- DTOs -----
@JsonClass(generateAdapter = true)
data class ChatRequest(
    val model: String = "gpt-4o-mini",              // small, fast model; adjust as needed
    val messages: List<Message>,
    val temperature: Double = 0.2
)
@JsonClass(generateAdapter = true) data class Message(val role: String, val content: String)
@JsonClass(generateAdapter = true)
data class ChatResponse(
    val choices: List<Choice>
) {
    @JsonClass(generateAdapter = true)
    data class Choice(val index: Int, val message: Message)
}

// ----- API -----
interface OpenAiApi {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun chat(@Body req: ChatRequest): ChatResponse
}

// ----- Builder -----
class BearerAuth(private val keyProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val key = keyProvider()
        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $key")
            .build()
        return chain.proceed(req)
    }
}

object OpenAiService {
    fun create(keyProvider: () -> String): OpenAiApi {
        val client = OkHttpClient.Builder()
            .addInterceptor(BearerAuth(keyProvider))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")      // official endpoint
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()
        return retrofit.create(OpenAiApi::class.java)
    }
}
