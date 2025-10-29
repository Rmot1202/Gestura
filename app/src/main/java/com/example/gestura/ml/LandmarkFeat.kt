package com.example.gestura.ml

import com.example.gestura.network.FrameLM

/**
 * Make 126-D feature per frame: [63 left] + [63 right]; zeros if a hand is missing.
 */
fun to126D(frames: List<FrameLM>): List<FloatArray> {
    val out = ArrayList<FloatArray>(frames.size)
    for (f in frames) {
        val v = FloatArray(126)
        // left
        if (f.lh != null && f.lh.size == 63) {
            for (i in 0 until 63) v[i] = f.lh[i]
        }
        // right
        if (f.rh != null && f.rh.size == 63) {
            for (i in 0 until 63) v[63 + i] = f.rh[i]
        }
        out.add(v)
    }
    return out
}
