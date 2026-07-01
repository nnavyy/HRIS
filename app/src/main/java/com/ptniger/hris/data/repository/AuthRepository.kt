package com.ptniger.hris.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.ptniger.hris.data.model.User
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Login gagal")
            val doc = db.collection(Constants.Collections.USERS).document(uid).get().await()
            if (doc.exists()) {
                val user = doc.toObject(User::class.java)?.copy(userId = uid)
                    ?: throw Exception("Data user tidak ditemukan")
                Result.success(user)
            } else {
                throw Exception("User belum terdaftar di sistem")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserData(): Result<User> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Belum login")
            val doc = db.collection(Constants.Collections.USERS).document(uid).get().await()
            val user = doc.toObject(User::class.java)?.copy(userId = uid)
                ?: throw Exception("Data user tidak ditemukan")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserAccount(email: String, password: String, user: User): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Gagal buat akun")
            val userData = user.copy(userId = uid)
            db.collection(Constants.Collections.USERS).document(uid).set(userData).await()
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUserByAdmin(context: android.content.Context, email: String, password: String, user: User): Result<String> {
        var secondaryApp: com.google.firebase.FirebaseApp? = null
        var createdUser: FirebaseUser? = null
        return try {
            val options = com.google.firebase.FirebaseApp.getInstance().options
            secondaryApp = com.google.firebase.FirebaseApp.getApps(context).find { it.name == "secondaryApp" }
            if (secondaryApp == null) {
                secondaryApp = com.google.firebase.FirebaseApp.initializeApp(context, options, "secondaryApp")
            }
            val secondaryAuth = com.google.firebase.auth.FirebaseAuth.getInstance(secondaryApp!!)

            val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
            createdUser = result.user
            val uid = createdUser?.uid ?: throw Exception("Gagal buat akun")

            try {
                val userData = user.copy(userId = uid, uid = uid)
                db.collection(Constants.Collections.USERS).document(uid).set(userData).await()
                Result.success(uid)
            } catch (firestoreError: Exception) {
                // Rollback auth user if firestore fails
                createdUser?.delete()?.await()
                throw Exception("Gagal menyimpan data user: ${firestoreError.message}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            secondaryApp?.delete()
        }
    }

    suspend fun getAllUsers(): List<User> {
        return try {
            val docs = db.collection(Constants.Collections.USERS).get().await()
            docs.documents.mapNotNull { it.toObject(User::class.java)?.copy(userId = it.id) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            db.collection(Constants.Collections.USERS).document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() { auth.signOut() }
}
