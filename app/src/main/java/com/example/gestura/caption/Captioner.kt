package com.example.gestura.caption

import android.content.Context
import android.net.Uri
import com.example.gestura.ml.AslInterpreter
import com.example.gestura.ml.LandmarksExtractor
import com.example.gestura.video.FrameSampler
import kotlin.math.max
import kotlin.math.roundToInt

data class Segment(val start: Double, val end: Double, val token: String, val prob: Double)

object Captioner {
    private const val TARGET_FPS = 13          // match training
    private const val WINDOW = 25              // frames per sample
    private const val STRIDE = 12              // overlap
    private const val MIN_PROB = 0.55          // tune
    private const val MERGE_GAP_S = 0.30       // merge small gaps

    /**
     * Full pipeline (on-device): video -> frames -> landmarks -> TFLite windows
     * Returns (merged segments, durationSec)
     */
    fun run(context: Context, uri: Uri, featDim: Int): Pair<List<Segment>, Double> {
        val (frames, duration) = FrameSampler.sample(context, uri, TARGET_FPS)
        if (frames.isEmpty()) return emptyList<Segment>() to 0.0

        val extractor = LandmarksExtractor(context)
        val interpreter = AslInterpreter(context, WINDOW, featDim)

        // Per-frame features
        // Assuming extractor.handsFeatures(it) returns DoubleArray
        val feats = frames.map { extractor.handsFeatures(it) } // concat pose/face if used in training
        extractor.close()

        // Slide windows
        val segments = mutableListOf<Segment>()
        var i = 0
        while (i + WINDOW <= feats.size) {
            val windowFeat = FloatArray(WINDOW * featDim)
            var p = 0
            for (w in 0 until WINDOW) {
                val f = feats[i + w] // f is likely a DoubleArray
                // Original problematic line: System.arraycopy(f, 0, windowFeat, p, featDim); p += featDim

                // FIX: Manually copy and convert Double elements to Float
                for (k in 0 until featDim) {
                    windowFeat[p + k] = f[k].toFloat()
                }
                p += featDim
            }
            val (token, prob) = interpreter.predictWindow(windowFeat)
            val s = i / TARGET_FPS.toDouble()
            val e = (i + WINDOW) / TARGET_FPS.toDouble()
            if (prob >= MIN_PROB) segments.add(Segment(s, e, token, prob.toDouble()))
            i += STRIDE
        }
        interpreter.close()

        val merged = mergeRepeats(segments)
        return merged to duration
    }

    private fun mergeRepeats(segs: List<Segment>): List<Segment> {
        if (segs.isEmpty()) return segs
        val out = mutableListOf<Segment>()
        var cur = segs.first()
        for (k in 1 until segs.size) {
            val s = segs[k]
            if (s.token == cur.token && s.start - cur.end <= MERGE_GAP_S) {
                cur = cur.copy(end = s.end, prob = max(cur.prob, s.prob))
            } else {
                out.add(cur); cur = s
            }
        }
        out.add(cur)
        return out
    }

    // (Optional) SRT builder for token segments; we're using NLP for final captions instead.
    fun segmentsToSrt(segs: List<Segment>): String {
        fun fmt(t: Double): String {
            val totalMs = (t * 1000.0).roundToInt()
            val h = totalMs / 3600000
            val m = (totalMs % 3600000) / 60000
            val s = (totalMs % 60000) / 1000
            val ms = totalMs % 1000
            return "%02d:%02d:%02d,%03d".format(h, m, s, ms)
        }
        return buildString {
            for ((idx, seg) in segs.withIndex()) {
                appendLine(idx + 1)
                appendLine("${fmt(seg.start)} --> ${fmt(seg.end)}")
                appendLine(seg.token)
                appendLine()
            }
        }

    }
    fun classifyFromFeatures(features: List<FloatArray>, fps: Int): Pair<List<Segment>, Double> {
        // TODO: Implement your windowing + TFLite (or other) inference
        // and decode into (token, start, end). Example duration:
        val duration = if (fps > 0) features.size / fps.toDouble() else 0.0
        // Return your real segments here:
        return emptyList<Segment>() to duration
    }
}