// video/FrameSampler.kt
package com.example.gestura.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlin.math.roundToLong

object FrameSampler {
    /**
     * Extract bitmaps at targetFps (e.g., 13). Returns Pair(frames, durationSec).
     */
    fun sample(context: Context, uri: Uri, targetFps: Int): Pair<List<Bitmap>, Double> {
        val mmr = MediaMetadataRetriever()
        mmr.setDataSource(context, uri)
        val durMs = (mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L)
        val durationSec = durMs / 1000.0
        if (durMs == 0L) return emptyList<Bitmap>() to 0.0

        val frameTimesUs = sequence {
            val stepUs = (1_000_000.0 / targetFps).roundToLong()
            var t = 0L
            while (t < durMs * 1000L) {
                yield(t)
                t += stepUs
            }
        }.toList()

        val frames = frameTimesUs.mapNotNull { ts ->
            mmr.getFrameAtTime(ts, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
        mmr.release()
        return frames to durationSec
    }
}
