// ml/LandmarksExtractor.kt
package com.example.gestura.ml

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mlkit.genai.common.BaseOptions

class LandmarksExtractor(context: Context) {
    private val options = HandLandmarker.HandLandmarkerOptions.builder()
        .setBaseOptions(HandLandmarker.HandLandmarkerOptions.BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task") // download from MediaPipe Tasks
            .build())
        .setNumHands(2)
        .setRunningMode(RunningMode.IMAGE)
        .build()

    private val landmarker = HandLandmarker.createFromOptions(context, options)

    fun handsFeatures(bmp: Bitmap): FloatArray {
        val image: MPImage = BitmapImageBuilder(bmp).build()
        val res: HandLandmarkerResult = landmarker.detect(image)
        // Build a fixed-size vector: for 2 hands x 21 landmarks x (x,y,z) = 126 floats
        val features = FloatArray(2 * 21 * 3) { 0f }
        var offset = 0
        // left/right order must match training!
        val hands = res.handednesses().zip(res.landmarks()).sortedBy { it.first[0].categoryName() }
        for ((_, lmks) in hands.take(2)) {
            for (l in lmks) {
                features[offset++] = l.x()
                features[offset++] = l.y()
                features[offset++] = l.z()
            }
        }
        // If <2 hands detected, remaining stays zero; match training padding
        return features
    }

    fun close() { landmarker.close() }
}
