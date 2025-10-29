package com.example.gestura.ml

import android.content.Context
import org.json.JSONArray
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AslInterpreter(context: Context, private val window: Int, private val featDim: Int) {
    private val tflite: Interpreter
    private val vocab: List<String>

    init {
        val modelBytes = context.assets.open("asl_model.tflite").readBytes()
        // Convert ByteArray to ByteBuffer for the Interpreter constructor
        val modelByteBuffer = ByteBuffer.allocateDirect(modelBytes.size)
        modelByteBuffer.order(ByteOrder.nativeOrder()) // Set byte order to native
        modelByteBuffer.put(modelBytes) // Put the bytes into the buffer
        modelByteBuffer.rewind() // Rewind the buffer to the beginning for the interpreter

        tflite = Interpreter(modelByteBuffer)

        val vocabStr = String(context.assets.open("vocab.json").readBytes())
        val arr = JSONArray(vocabStr)
        vocab = List(arr.length()) { arr.getString(it) }
    }


    fun predictWindow(win: FloatArray): Pair<String, Float> {
        // win length = window * featDim
        val input = ByteBuffer.allocateDirect(4 * window * featDim).order(ByteOrder.nativeOrder())
        for (f in win) input.putFloat(f)
        input.rewind()

        val out = Array(1) { FloatArray(vocab.size) }
        tflite.run(input, out)

        val probs = out[0]
        var best = 0
        for (i in 1 until probs.size) if (probs[i] > probs[best]) best = i
        return vocab[best] to probs[best]
    }

    fun close() = tflite.close()
}
