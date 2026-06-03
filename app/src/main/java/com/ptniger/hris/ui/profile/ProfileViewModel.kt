package com.ptniger.hris.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Holds the latest uploaded photo URL so ProfileScreen can re-render without needing a User refresh
    private val _photoUrl = MutableStateFlow("")
    val photoUrl: StateFlow<String> = _photoUrl

    fun uploadProfilePicture(userId: String, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val ref = storage.reference.child("profile_pictures/$userId.jpg")
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                
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
    
    fun clearMessage() { _message.value = null }
}
