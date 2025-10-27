// ml/AslInterpreter.kt
package com.example.gestura.ml

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.json.JSONArray
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AslInterpreter(context: Context, private val window: Int = 25, private val featDim: Int) {
    private val tflite: Interpreter
    private val vocab: List<String>

    init {
        val modelBytes = context.assets.open("asl_model.tflite").readBytes()
        tflite = Interpreter(modelBytes)
        vocab = JSONArray(String(context.assets.open("vocab.json").readBytes()))
            .let { arr -> List(arr.length()) { arr.getString(it) } }
    }

    fun predictWindow(win: FloatArray): Pair<String, Float> {
        // win size = window * featDim
        val input = ByteBuffer.allocateDirect(4 * window * featDim).order(ByteOrder.nativeOrder())
        for (f in win) input.putFloat(f)
        input.rewind()

        // Output: [1, V] probabilities
        val out = Array(1) { FloatArray(vocab.size) }
        tflite.run(input, out)
        val probs = out[0]
        var best = 0
        for (i in 1 until probs.size) if (probs[i] > probs[best]) best = i
        return vocab[best] to probs[best]
    }

    fun close() = tflite.close()
}
