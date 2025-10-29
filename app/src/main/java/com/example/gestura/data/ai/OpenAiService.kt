package com.example.gestura.data.ai

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAiService {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun chat(@Body body: ChatRequest): ChatResponse

    companion object {
        /**
         * Usage: OpenAiService.create { BuildConfig.OPENAI_API_KEY }
         */
        fun create(
            keyProvider: () -> String,
            baseUrl: String = "https://api.openai.com/"
        ): OpenAiService {
            val auth = Interceptor { chain ->
                val key = keyProvider().trim()
                val req = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $key")
                    .build()
                chain.proceed(req)
            }

            val http = OkHttpClient.Builder()
                .addInterceptor(auth)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .client(http)
                .build()
                .create(OpenAiService::class.java)
        }
    }
}
