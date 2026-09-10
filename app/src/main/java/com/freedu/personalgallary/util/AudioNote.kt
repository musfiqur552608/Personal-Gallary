package com.freedu.personalgallary.util

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Voice-note recording/playback helper. Files live in app-private storage.
 * Recording needs RECORD_AUDIO (requested at runtime by the UI).
 */
object AudioNote {

    fun newFile(context: Context): File {
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        return File(dir, "VN_${System.currentTimeMillis()}.m4a")
    }

    /** Starts recording into [file]; returns the recorder or null on failure. */
    fun start(context: Context, file: File): MediaRecorder? {
        return try {
            @Suppress("DEPRECATION")
            val rec = if (Build.VERSION.SDK_INT >= 31) {
                MediaRecorder(context.applicationContext)
            } else {
                MediaRecorder()
            }
            rec.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun stop(rec: MediaRecorder?, file: File): Boolean {
        return try {
            rec?.apply { stop(); reset(); release() }
            file.exists() && file.length() > 0
        } catch (_: Exception) {
            try {
                rec?.release()
            } catch (_: Exception) {
            }
            false
        }
    }

    /** One-shot player. Returns player (caller must release on completion/change). */
    fun play(path: String, onDone: () -> Unit): MediaPlayer? {
        return try {
            MediaPlayer().apply {
                setDataSource(path)
                prepare()
                setOnCompletionListener { onDone() }
                start()
            }
        } catch (_: Exception) {
            null
        }
    }
}
