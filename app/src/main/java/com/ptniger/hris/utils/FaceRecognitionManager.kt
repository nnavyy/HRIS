package com.ptniger.hris.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.ptniger.hris.data.model.Employee
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manager untuk face detection + recognition.
 *
 * Alur verifikasi:
 * 1. Detect wajah di frame kamera (ML Kit)
 * 2. Liveness check: user harus berkedip 1x
 * 3. Extract embedding dari frame terbaik (FaceNet)
 * 4. Bandingkan dengan embedding karyawan (cosine similarity)
 * 5. Return hasil: MATCH / NO_MATCH / NO_FACE / LIVENESS_FAILED
 */
object FaceRecognitionManager {

    // ML Kit Face Detector — aktifkan classification untuk liveness
    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL) // untuk blink
                .setMinFaceSize(0.20f)  // minimal 20% frame
                .enableTracking()
                .build()
        )
    }

    sealed class RecognitionResult {
        object NoFace : RecognitionResult()
        object LivenessFailed : RecognitionResult()
        data class Match(val employee: Employee, val similarity: Float) : RecognitionResult()
        data class NoMatch(val similarity: Float, val bestMatch: Employee?) : RecognitionResult()
        data class Error(val message: String) : RecognitionResult()
    }

    /**
     * Deteksi wajah di bitmap.
     * Return list of Face objects dari ML Kit.
     */
    suspend fun detectFaces(bitmap: Bitmap): List<Face> =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            detector.process(image)
                .addOnSuccessListener { faces -> cont.resume(faces) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    /**
     * Versi synchronous/callback untuk mendeteksi wajah di bitmap.
     * Tidak memblokir coroutine atau memaksa bikin scope baru tiap frame.
     */
    fun detectFacesSync(bitmap: Bitmap, onResult: (List<Face>) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { faces -> onResult(faces) }
            .addOnFailureListener { onResult(emptyList()) }
    }

    /**
     * Crop bitmap ke bounding box wajah (dengan padding 20%).
     */
    fun cropFaceBitmap(bitmap: Bitmap, face: Face): Bitmap {
        val bounds = face.boundingBox
        val padding = (bounds.width() * 0.20).toInt()
        val left   = (bounds.left - padding).coerceAtLeast(0)
        val top    = (bounds.top - padding).coerceAtLeast(0)
        val right  = (bounds.right + padding).coerceAtMost(bitmap.width)
        val bottom = (bounds.bottom + padding).coerceAtMost(bitmap.height)
        val width  = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    /**
     * Verifikasi wajah terhadap daftar karyawan.
     * Cari karyawan dengan similarity tertinggi.
     *
     * @param faceBitmap Bitmap wajah yang sudah di-crop
     * @param employees Daftar karyawan yang punya faceEmbedding
     * @param model FaceNetModel yang sudah diinit
     */
    fun verifyFace(
        faceBitmap: Bitmap,
        employees: List<Employee>,
        model: FaceNetModel
    ): RecognitionResult {
        val registeredEmployees = employees.filter { it.isFaceRegistered && it.faceEmbedding.isNotEmpty() }
        if (registeredEmployees.isEmpty()) {
            return RecognitionResult.Error("Belum ada karyawan yang mendaftarkan wajah.")
        }

        val queryEmbedding = model.getEmbedding(faceBitmap)
        var bestSimilarity = 0f
        var bestEmployee: Employee? = null

        registeredEmployees.forEach { emp ->
            val storedEmbedding = emp.faceEmbedding.toFloatArray()
            val similarity = model.cosineSimilarity(queryEmbedding, storedEmbedding)
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity
                bestEmployee = emp
            }
        }

        return if (bestSimilarity >= FaceNetModel.SIMILARITY_THRESHOLD) {
            RecognitionResult.Match(bestEmployee!!, bestSimilarity)
        } else {
            RecognitionResult.NoMatch(bestSimilarity, bestEmployee)
        }
    }

    /**
     * Jenis tantangan acak untuk Active Liveness Detection (Anti-Spoofing).
     * Mencegah pemakaian foto cetak atau video layar HP orang lain.
     */
    enum class LivenessChallengeType(val instruction: String, val hint: String) {
        TURN_LEFT("Tolehkan kepala sedikit ke KIRI", "Arahkan wajah ke kiri Anda"),
        TURN_RIGHT("Tolehkan kepala sedikit ke KANAN", "Arahkan wajah ke kanan Anda"),
        SMILE("Tersenyum ke kamera", "Tunjukkan senyum Anda"),
        BLINK("Kedipkan kedua mata", "Tutup mata sejenak lalu buka")
    }

    fun getRandomChallenge(): LivenessChallengeType {
        val challenges = listOf(
            LivenessChallengeType.TURN_LEFT,
            LivenessChallengeType.TURN_RIGHT,
            LivenessChallengeType.SMILE
        )
        return challenges.random()
    }

    /**
     * Memverifikasi apakah wajah pengguna memenuhi tantangan liveness aktif yang diminta.
     */
    fun verifyChallenge(face: Face, challenge: LivenessChallengeType): Boolean {
        return when (challenge) {
            LivenessChallengeType.TURN_LEFT -> isTurningLeft(face)
            LivenessChallengeType.TURN_RIGHT -> isTurningRight(face)
            LivenessChallengeType.SMILE -> isSmiling(face)
            LivenessChallengeType.BLINK -> isBlinking(face)
        }
    }

    fun isTurningLeft(face: Face): Boolean {
        // Pada kamera depan, toleh ke kiri user biasanya menghasilkan sudut Y negatif
        return face.headEulerAngleY < -12f
    }

    fun isTurningRight(face: Face): Boolean {
        return face.headEulerAngleY > 12f
    }

    fun isSmiling(face: Face): Boolean {
        val smile = face.smilingProbability ?: 0f
        return smile > 0.60f
    }

    fun isFaceForward(face: Face): Boolean {
        val y = kotlin.math.abs(face.headEulerAngleY)
        val z = kotlin.math.abs(face.headEulerAngleZ)
        return y < 10f && z < 10f
    }

    /**
     * Liveness check: cek apakah user sudah berkedip.
     * ML Kit memberikan `leftEyeOpenProbability` dan `rightEyeOpenProbability` (0.0-1.0).
     * Blink terdeteksi ketika keduanya < 0.35 (mata tertutup).
     */
    fun isBlinking(face: Face): Boolean {
        val leftOpen  = face.leftEyeOpenProbability ?: 1f
        val rightOpen = face.rightEyeOpenProbability ?: 1f
        return leftOpen < 0.35f && rightOpen < 0.35f
    }
}
