package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import kotlin.math.sin

enum class AlarmSoundType(val id: String, val displayName: String, val description: String) {
    SYSTEM_ALARM("system_alarm", "Default Alarm Tone", "Standard Android system alarm"),
    ARACHNID_PULSE("arachnid_pulse", "🕷️ Arachnid Pulse", "Rhythmic rapid pulse designed for heavy sleepers"),
    GOTHIC_BELL("gothic_bell", "🔔 Gothic Cathedral Bell", "Deep resonant bell strikes with dark overtones"),
    DIGITAL_SIREN("digital_siren", "🚨 Web Siren Alert", "High-frequency sweeping cyber warning siren"),
    STEAMPUNK_CHIME("steampunk_chime", "🕰️ Steampunk Clock Chime", "Antique mechanical gear chime melody"),
    SYSTEM_RINGTONE("system_ringtone", "System Ringtone", "Default incoming ringtone")
}

object SoundManager {
    private const val TAG = "SoundManager"
    private var previewTrack: AudioTrack? = null
    private var previewPlayer: MediaPlayer? = null
    private var isLooping = false
    private var synthThread: Thread? = null

    fun playSound(context: Context, soundType: AlarmSoundType, loop: Boolean = false) {
        stopSound()
        isLooping = loop

        when (soundType) {
            AlarmSoundType.SYSTEM_ALARM, AlarmSoundType.SYSTEM_RINGTONE -> {
                try {
                    val uri: Uri? = if (soundType == AlarmSoundType.SYSTEM_ALARM) {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    } else {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    }

                    if (uri != null) {
                        previewPlayer = MediaPlayer().apply {
                            setDataSource(context, uri)
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_ALARM)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                            isLooping = loop
                            prepare()
                            start()
                        }
                        return
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play system uri, falling back to synth", e)
                }
                playSynthesized(soundType, loop)
            }
            else -> {
                playSynthesized(soundType, loop)
            }
        }
    }

    private fun playSynthesized(soundType: AlarmSoundType, loop: Boolean) {
        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
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
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        previewTrack = track
        track.play()

        synthThread = Thread {
            val samples = ShortArray(sampleRate / 4) // 250ms chunks
            var phase = 0.0
            var tick = 0

            while (isLooping || tick < 8) { // If not looping, play 2 seconds
                val freq = when (soundType) {
                    AlarmSoundType.ARACHNID_PULSE -> if ((tick % 2) == 0) 880.0 else 587.33
                    AlarmSoundType.GOTHIC_BELL -> 220.0 + (if ((tick % 4) == 0) 0.0 else 110.0)
                    AlarmSoundType.DIGITAL_SIREN -> 600.0 + ((tick % 8) * 120.0)
                    AlarmSoundType.STEAMPUNK_CHIME -> when (tick % 4) {
                        0 -> 523.25
                        1 -> 659.25
                        2 -> 783.99
                        else -> 1046.50
                    }
                    else -> 440.0
                }

                for (i in samples.indices) {
                    val envelope = if (soundType == AlarmSoundType.GOTHIC_BELL) {
                        1.0 - (i.toDouble() / samples.size)
                    } else {
                        0.9
                    }
                    samples[i] = (sin(phase) * 30000 * envelope).toInt().toShort()
                    phase += 2.0 * Math.PI * freq / sampleRate
                    if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                }

                track.write(samples, 0, samples.size)
                tick++
            }
        }.apply { start() }
    }

    fun stopSound() {
        isLooping = false
        synthThread?.interrupt()
        synthThread = null

        try {
            previewTrack?.stop()
            previewTrack?.release()
        } catch (_: Exception) {}
        previewTrack = null

        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) {}
        previewPlayer = null
    }
}
