package com.example.gestura.network

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class FrameLM(
    val t: Double,
    val lh: List<Float>?,   // 63 or null
    val rh: List<Float>?    // 63 or null
)
data class HolisticResp(
    val fps: Int,
    val duration: Double,
    val frames: List<FrameLM>
)

interface HolisticApi {
    @Multipart
    @POST("holistic/landmarks")
    suspend fun landmarks(@Part video: MultipartBody.Part): HolisticResp
}
