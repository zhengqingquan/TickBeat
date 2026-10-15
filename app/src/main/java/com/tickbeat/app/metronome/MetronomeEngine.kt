package com.tickbeat.app.metronome

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * AudioTrack-based metronome. Writes click + silence in a dedicated thread
 * so timing follows the audio stream instead of UI timers.
 */
class MetronomeEngine {

    companion object {
        private const val SAMPLE_RATE = 44_100
        private const val CLICK_MS = 18
        private const val ACCENT_HZ = 1760.0
        private const val NORMAL_HZ = 880.0
    }

    private val playing = AtomicBoolean(false)
    private val bpm = AtomicInteger(120)
    private val beatsPerBar = AtomicInteger(4)

    @Volatile
    private var currentBeat: Int = 0

    @Volatile
    var onBeat: ((beatIndex: Int) -> Unit)? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var worker: Thread? = null
    private var track: AudioTrack? = null

    private val accentClick = synthesizeClick(ACCENT_HZ)
    private val normalClick = synthesizeClick(NORMAL_HZ)

    fun getBpm(): Int = bpm.get()
    fun getBeatsPerBar(): Int = beatsPerBar.get()
    fun isPlaying(): Boolean = playing.get()
    fun getCurrentBeat(): Int = currentBeat

    fun setBpm(value: Int) {
        bpm.set(value.coerceIn(30, 300))
    }

    fun setBeatsPerBar(value: Int) {
        beatsPerBar.set(value.coerceIn(1, 12))
    }

    @Synchronized
    fun start() {
        if (playing.get()) return
        playing.set(true)
        currentBeat = 0

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = minBuf.coerceAtLeast(SAMPLE_RATE / 10)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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
            .setBufferSizeInBytes(bufferSize)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        track = audioTrack
        audioTrack.play()

        worker = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            runLoop(audioTrack)
        }, "MetronomeEngine").also {
            it.isDaemon = true
            it.start()
        }
    }

    @Synchronized
    fun stop() {
        playing.set(false)
        worker?.join(500)
        worker = null
        track?.let {
            runCatching {
                it.stop()
                it.release()
            }
        }
        track = null
        currentBeat = 0
        onBeat?.invoke(0)
    }

    private fun runLoop(audioTrack: AudioTrack) {
        val silenceChunk = ShortArray(512)
        var samplesUntilClick = 0
        var beat = 0

        while (playing.get()) {
            val samplesPerBeat = ((SAMPLE_RATE * 60.0) / bpm.get()).toInt().coerceAtLeast(1)

            if (samplesUntilClick <= 0) {
                val bar = beatsPerBar.get().coerceAtLeast(1)
                val isAccent = beat % bar == 0
                val click = if (isAccent) accentClick else normalClick
                writeFully(audioTrack, click)
                currentBeat = (beat % bar) + 1
                val beatIndex = currentBeat
                mainHandler.post { onBeat?.invoke(beatIndex) }
                beat++
                samplesUntilClick = (samplesPerBeat - click.size).coerceAtLeast(0)
            }

            val toWrite = min(silenceChunk.size, samplesUntilClick)
            if (toWrite > 0) {
                writeFully(audioTrack, silenceChunk, toWrite)
                samplesUntilClick -= toWrite
            }
        }
    }

    private fun writeFully(track: AudioTrack, data: ShortArray, length: Int = data.size) {
        var offset = 0
        while (offset < length && playing.get()) {
            val written = track.write(data, offset, length - offset)
            if (written <= 0) break
            offset += written
        }
    }

    private fun synthesizeClick(frequencyHz: Double): ShortArray {
        val length = SAMPLE_RATE * CLICK_MS / 1000
        val samples = ShortArray(length)
        for (i in 0 until length) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = 1.0 - (i.toDouble() / length)
            val sample = sin(2.0 * PI * frequencyHz * t) * envelope
            samples[i] = (sample * Short.MAX_VALUE * 0.55).toInt().toShort()
        }
        return samples
    }
}
