package com.ptniger.hris.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Holds the latest uploaded photo URL so ProfileScreen can re-render without needing a User refresh
    private val _photoUrl = MutableStateFlow("")
    val photoUrl: StateFlow<String> = _photoUrl

    companion object {
        // Same Cloudinary config as AttendanceRepository
        private const val CLOUD_NAME = "dxn0pj04j"
        private const val UPLOAD_PRESET = "hris_upload"
    }

    fun uploadProfilePicture(userId: String, uri: Uri, context: Context? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get the context from the application
                val appContext = context ?: throw Exception("Context diperlukan untuk upload")
                
                // Read image bytes
                val inputStream = appContext.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes() ?: throw Exception("Tidak bisa membaca gambar")
                inputStream.close()

                // Upload to Cloudinary
                val downloadUrl = withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", "profile_$userId.jpg", bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
                        .addFormDataPart("upload_preset", UPLOAD_PRESET)
                        .addFormDataPart("public_id", "profile_pictures/$userId")
                        .build()

                    val request = Request.Builder()
                        .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                        .post(requestBody)
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    if (response.isSuccessful && responseBody != null) {
                        val json = JSONObject(responseBody)
                        json.getString("secure_url")
                    } else {
                        throw Exception("Cloudinary upload failed: $responseBody")
                    }
                }

                // Save URL to Firestore (not the image itself, just the URL)
                db.collection(Constants.Collections.USERS).document(userId)
                    .update("photoUrl", downloadUrl).await()
                
                // Update local state so UI reacts immediately
                _photoUrl.value = downloadUrl
                _message.value = "Foto profil berhasil diperbarui"
            } catch (e: Exception) {
                _message.value = "Gagal mengunggah foto: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateProfileInfo(userId: String, updates: Map<String, Any>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                db.collection(Constants.Collections.USERS).document(userId).update(updates).await()
                _message.value = "Profil berhasil diperbarui"
                onComplete(true)
            } catch (e: Exception) {
                _message.value = "Gagal memperbarui profil: ${e.message}"
                onComplete(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearMessage() { _message.value = null }
}
