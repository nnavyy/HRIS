package com.ptniger.hris.utils

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

/**
 * Wrapper untuk FaceNet TFLite model.
 * Input: Bitmap wajah (112x112 pixel)
 * Output: embedding vector 128 float (face signature)
 */
class FaceNetModel(context: Context) {

    private val interpreter: Interpreter

    init {
        val assetFileDescriptor = context.assets.openFd("mobile_face_net.tflite")
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        val options = Interpreter.Options().apply { numThreads = 4 }
        interpreter = Interpreter(modelBuffer, options)
    }

    /**
     * Extract 128-float embedding dari face bitmap.
     * Bitmap HARUS sudah di-crop ke area wajah saja (hasil dari ML Kit).
     */
    fun getEmbedding(faceBitmap: Bitmap): FloatArray {
        // Resize ke 112x112 (input size FaceNet)
        val resized = Bitmap.createScaledBitmap(faceBitmap, 112, 112, true)

        // Convert ke ByteBuffer (normalized -1.0 to 1.0)
        val inputBuffer = ByteBuffer.allocateDirect(1 * 112 * 112 * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.rewind()

        for (y in 0 until 112) {
            for (x in 0 until 112) {
                val pixel = resized.getPixel(x, y)
                val r = ((pixel shr 16 and 0xFF) - 128f) / 128f
                val g = ((pixel shr 8 and 0xFF) - 128f) / 128f
                val b = ((pixel and 0xFF) - 128f) / 128f
                inputBuffer.putFloat(r)
                inputBuffer.putFloat(g)
                inputBuffer.putFloat(b)
            }
        }

        // Run inference
        val output = Array(1) { FloatArray(128) }
        interpreter.run(inputBuffer, output)

        // L2 normalize embedding
        return l2Normalize(output[0])
    }

    private fun l2Normalize(vec: FloatArray): FloatArray {
        val norm = sqrt(vec.map { it * it }.sum())
        return if (norm > 0) vec.map { it / norm }.toFloatArray() else vec
    }

    /**
     * Hitung cosine similarity antara dua embedding.
     * Return: 0.0 (berbeda total) hingga 1.0 (identik)
     * Threshold yang disarankan: 0.85 untuk match
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dotProduct / denom
    }

    fun close() { interpreter.close() }

    companion object {
        const val SIMILARITY_THRESHOLD = 0.82f    // match threshold
        const val LIVENESS_BLINK_COUNT = 1        // minimal kedip untuk liveness
    }
}
