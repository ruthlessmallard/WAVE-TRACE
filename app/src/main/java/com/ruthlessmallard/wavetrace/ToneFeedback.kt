package com.ruthlessmallard.wavetrace

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * Short rising / falling chirps that report a meaningful change in signal.
 *
 * Rising  = signal got stronger (RSSI moved toward 0 dBm).
 * Falling = signal got weaker.
 *
 * Implemented as pure PCM sine sweeps via [AudioTrack] — no bundled assets and
 * no third-party dependencies. Both chirps are synthesised once, kept in static
 * mode, and replayed cheaply on demand.
 *
 * WAVE-TRACE is not a safety device; this is a convenience cue only.
 */
class ToneFeedback(private val volume: Float = 0.5f) {

    private val sampleRate = 44100
    private var risingTrack: AudioTrack? = null
    private var fallingTrack: AudioTrack? = null

    fun rising() {
        if (risingTrack == null) risingTrack = buildChirp(from = 1000.0, to = 1600.0)
        play(risingTrack)
    }

    fun falling() {
        if (fallingTrack == null) fallingTrack = buildChirp(from = 1600.0, to = 1000.0)
        play(fallingTrack)
    }

    private fun play(track: AudioTrack?) {
        val t = track ?: return
        try {
            if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.stop()
            t.reloadStaticData()
            t.play()
        } catch (_: IllegalStateException) {
            // Audio focus or device changed under us; a dropped cue is not fatal.
        }
    }

    /**
     * Short percussive CLICK, not a whoop. A click is a transient: brief,
     * sharp attack, fast decay. Rising/falling character comes from a fast
     * pitch sweep across the few milliseconds it lasts.
     */
    private fun buildChirp(from: Double, to: Double, durationMs: Int = 18): AudioTrack {
        val n = sampleRate * durationMs / 1000
        val samples = ShortArray(n)
        var phase = 0.0
        val decaySamples = sampleRate * 0.004 // ~4 ms exponential tail
        for (i in 0 until n) {
            val frac = i.toDouble() / n
            val freq = from + (to - from) * frac
            phase += 2.0 * Math.PI * freq / sampleRate
            // 1 ms attack ramp (no DC pop) x exponential decay (transient snap)
            val attack = (i / (sampleRate * 0.001)).coerceAtMost(1.0)
            val decay = Math.exp(-i / decaySamples)
            samples[i] = (Math.sin(phase) * attack * decay * volume * Short.MAX_VALUE)
                .toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        return track
    }

    fun release() {
        risingTrack?.release()
        risingTrack = null
        fallingTrack?.release()
        fallingTrack = null
    }
}
