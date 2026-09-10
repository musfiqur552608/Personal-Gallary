package com.freedu.personalgallary.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * On-device face analysis (ML Kit). The model ships via Play services and may
 * need a one-time download — failures surface as [Result.failure] with a
 * friendly path for the UI, never a crash.
 */
object FaceScan {

    data class FaceInfo(val count: Int, val smiling: Boolean)

    private val detector by lazy {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
            .let { FaceDetection.getClient(it) }
    }

    suspend fun analyze(context: Context, uri: Uri): Result<FaceInfo> {
        return try {
            val image = InputImage.fromFilePath(context, uri)
            suspendCancellableCoroutine { cont ->
                detector.process(image)
                    .addOnSuccessListener { faces ->
                        val smile = faces.any { (it.smilingProbability ?: 0f) > 0.7f }
                        if (!cont.isCompleted) cont.resume(Result.success(FaceInfo(faces.size, smile)))
                    }
                    .addOnFailureListener { e ->
                        if (!cont.isCompleted) cont.resume(Result.failure(e))
                    }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Bounding boxes in image pixels (for blurring / cropping). */
    suspend fun boxes(context: Context, uri: Uri): Result<List<android.graphics.Rect>> {
        return try {
            val fast = FaceDetection.getClient(
                FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                    .build()
            )
            val image = InputImage.fromFilePath(context, uri)
            suspendCancellableCoroutine { cont ->
                fast.process(image)
                    .addOnSuccessListener { faces ->
                        if (!cont.isCompleted) {
                            cont.resume(Result.success(faces.map { it.boundingBox }))
                        }
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
