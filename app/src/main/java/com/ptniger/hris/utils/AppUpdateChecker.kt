package com.ptniger.hris.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class AppUpdateInfo(
    val latestVersionCode: Int = 0,
    val latestVersionName: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val isForceUpdate: Boolean = false
)

class AppUpdateChecker(private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    suspend fun checkForUpdate(): AppUpdateInfo? {
        return try {
            val snapshot = firestore.collection("app_config")
                .document("version_info")
                .get()
                .await()
            
            if (snapshot.exists()) {
                snapshot.toObject(AppUpdateInfo::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun openDownloadLink(context: Context, url: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
