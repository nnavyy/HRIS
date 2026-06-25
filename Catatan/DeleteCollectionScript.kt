import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DeleteCollectionScript {
    
    /**
     * Fungsi untuk menghapus seluruh dokumen di dalam sebuah collection.
     * Ganti "NAMA_COLLECTION_DISINI" dengan nama collection yang ingin dihapus.
     * 
     * Cara memanggil fungsi ini (misalnya diletakkan pada onClick sebuah Button):
     * CoroutineScope(Dispatchers.IO).launch {
     *     DeleteCollectionScript.deleteAllDataInCollection()
     * }
     */
    suspend fun deleteAllDataInCollection() {
        // HARDCODE nama collection di sini:
        val collectionName = "NAMA_COLLECTION_DISINI" // contoh: "employees" atau "payrolls"
        
        val db = FirebaseFirestore.getInstance()
        val colRef = db.collection(collectionName)
        
        try {
            val snapshot = colRef.get().await()
            if (snapshot.isEmpty) {
                Log.d("DeleteScript", "Collection $collectionName sudah kosong atau tidak ditemukan.")
                return
            }
            
            // Loop untuk menghapus setiap dokumen satu per satu
            for (document in snapshot.documents) {
                document.reference.delete().await()
                Log.d("DeleteScript", "Dokumen ${document.id} berhasil dihapus.")
            }
            Log.d("DeleteScript", "Berhasil menghapus total ${snapshot.size()} dokumen dari collection $collectionName")
        } catch (e: Exception) {
            Log.e("DeleteScript", "Gagal menghapus collection $collectionName", e)
        }
    }
}
