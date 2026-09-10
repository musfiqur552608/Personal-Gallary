package com.freedu.personalgallary.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device OCR (ML Kit Latin recognizer). Indexed text powers text search.
 * Model issues surface as [Result.failure] for graceful UI messaging.
 */
object OcrScan {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.Builder().build())
    }

    suspend fun read(context: Context, uri: Uri, maxChars: Int = 2000): Result<String> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val t = visionText.text.trim().take(maxChars)
                        if (!cont.isCompleted) cont.resume(Result.success(t))
                    }
                    .addOnFailureListener { e ->
                        if (!cont.isCompleted) cont.resume(Result.failure(e))
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
