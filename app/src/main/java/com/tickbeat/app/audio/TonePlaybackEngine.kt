package com.tickbeat.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Plays short pitched tones via AudioTrack (sine + AD envelope).
 */
class TonePlaybackEngine {

    companion object {
        private const val SAMPLE_RATE = 44_100
        private const val DEFAULT_DURATION_MS = 450
        private const val ATTACK_MS = 8
        private const val RELEASE_MS = 60
    }

    private val playing = AtomicBoolean(false)
    private val generation = AtomicInteger(0)

    private var worker: Thread? = null
    private var track: AudioTrack? = null

    fun playNote(midi: Int, durationMs: Int = DEFAULT_DURATION_MS) {
        stop()
        playing.set(true)
        val gen = generation.incrementAndGet()
        val hz = midiToHz(midi)
        val samples = synthesizeTone(hz, durationMs.coerceIn(80, 2_000))

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuf.coerceAtLeast(samples.size * 2))
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        track = audioTrack
        audioTrack.play()

        worker = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            if (generation.get() == gen && playing.get()) {
                writeFully(audioTrack, samples)
            }
            if (generation.get() == gen) {
                playing.set(false)
            }
        }, "TonePlayback").also {
            it.isDaemon = true
            it.start()
        }
    }

    @Synchronized
    fun stop() {
        playing.set(false)
        generation.incrementAndGet()

        val endingWorker = worker
        val endingTrack = track
        worker = null
        track = null
        if (endingWorker == null && endingTrack == null) return

        Thread({
            endingWorker?.join(500)
            endingTrack?.let {
                runCatching {
                    it.pause()
                    it.flush()
                    it.stop()
                    it.release()
                }
            }
        }, "TonePlayback-stop").apply {
            isDaemon = true
            start()
        }
    }

    private fun writeFully(track: AudioTrack, data: ShortArray) {
        var offset = 0
        while (offset < data.size && playing.get()) {
            val written = track.write(data, offset, data.size - offset)
            if (written <= 0) break
            offset += written
        }
    }

    private fun midiToHz(midi: Int): Double =
        440.0 * 2.0.pow((midi - 69) / 12.0)

    private fun synthesizeTone(hz: Double, durationMs: Int): ShortArray {
        val length = SAMPLE_RATE * durationMs / 1000
        val attack = (SAMPLE_RATE * ATTACK_MS / 1000).coerceAtMost(length / 4)
        val release = (SAMPLE_RATE * RELEASE_MS / 1000).coerceAtMost(length / 3)
        val samples = ShortArray(length)
        for (i in 0 until length) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = when {
                i < attack -> i.toDouble() / attack
                i > length - release -> (length - i).toDouble() / release
                else -> 1.0
            }
            samples[i] = (sin(2.0 * PI * hz * t) * env * Short.MAX_VALUE * 0.45).toInt().toShort()
        }
        return samples
    }
}
