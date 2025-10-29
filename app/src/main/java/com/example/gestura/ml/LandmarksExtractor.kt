package com.example.gestura.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Hands-only example. If your model used pose/face too,
 * add PoseLandmarker / FaceLandmarker in the same style and CONCAT features.
 */
class LandmarksExtractor(context: Context) {
    private val options = HandLandmarker.HandLandmarkerOptions.builder()
        .setBaseOptions(
            // CORRECTED LINE: Use BaseOptions.builder() directly
            BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()
        )
        .setNumHands(2)
        .setRunningMode(RunningMode.IMAGE)
        .build()

    private val landmarker = HandLandmarker.createFromOptions(context, options)

    /** Returns fixed-size vector: 2 hands × 21 landmarks × (x,y,z) = 126 floats */
    fun handsFeatures(bmp: Bitmap): FloatArray {
        val image: MPImage = BitmapImageBuilder(bmp).build()
        val res: HandLandmarkerResult = landmarker.detect(image)

        val features = FloatArray(2 * 21 * 3) { 0f }
        var offset = 0

        // Ensures stable left/right order if available
        val pairs = res.handednesses().zip(res.landmarks())
            .sortedBy { it.first.getOrNull(0)?.categoryName() ?: "Z" }

        for ((_, lms) in pairs.take(2)) {
            for (l in lms) {
                features[offset++] = l.x()
                features[offset++] = l.y()
                features[offset++] = l.z()
            }
        }
        return features
    }

    fun close() { landmarker.close() }
}