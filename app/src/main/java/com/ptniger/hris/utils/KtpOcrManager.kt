package com.ptniger.hris.utils

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object KtpOcrManager {
    data class OcrResult(
        val nik: String = "",
        val name: String = ""
    )

    fun extractKtpData(bitmap: Bitmap, onResult: (OcrResult) -> Unit, onError: (Exception) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val lines = visionText.textBlocks.flatMap { it.lines }.map { it.text }
                var nik = ""
                var name = ""
                
                // Regex for 16-digit NIK
                val nikRegex = Regex("\\b\\d{16}\\b")
                
                for (i in lines.indices) {
                    val line = lines[i]
                    
                    if (nik.isEmpty()) {
                        val match = nikRegex.find(line.replace(" ", "").replace("D", "0").replace("O", "0").replace("?", "7").replace("!", "1"))
                        if (match != null) {
                            nik = match.value
                        } else if (line.contains("NIK", ignoreCase = true)) {
                            val parts = line.split(":")
                            if (parts.size > 1 && parts[1].replace(Regex("[^0-9]"), "").length >= 16) {
                                nik = parts[1].replace(Regex("[^0-9]"), "").take(16)
                            } else if (i + 1 < lines.size) {
                                val nextLineNums = lines[i+1].replace(Regex("[^0-9]"), "")
                                if (nextLineNums.length >= 16) nik = nextLineNums.take(16)
                            }
                        }
                    }

                    if (name.isEmpty() && line.contains("Nama", ignoreCase = true)) {
                        val parts = line.split(":")
                        if (parts.size > 1 && parts[1].trim().length > 2) {
                            name = parts[1].trim()
                        } else if (i + 1 < lines.size) {
                            name = lines[i+1].replace(":", "").trim()
                        }
                    }
                }
                
                onResult(OcrResult(nik, name))
            }
            .addOnFailureListener { e ->
                onError(e)
            }
    }
}
