package com.example.gestura.network

import android.content.Context
import android.net.Uri
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.source
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object HolisticClient {

    fun api(baseUrl: String): HolisticApi {
        val http = OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

        // Moshi with Kotlin adapter so Retrofit can convert your Kotlin data classes
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(http)
            .build()
            .create(HolisticApi::class.java)
    }

    /** Stream the picked video into the multipart body (no big RAM buffers). */
    fun asStreamingPart(context: Context, uri: Uri): MultipartBody.Part {
        val rb = object : RequestBody() {
            override fun contentType() = "video/*".toMediaType()
            override fun writeTo(sink: okio.BufferedSink) {
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    sink.writeAll(input.source())
                }
            }
        }
        return MultipartBody.Part.createFormData("video", "upload.mp4", rb)
    }
}
