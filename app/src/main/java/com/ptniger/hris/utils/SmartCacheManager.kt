package com.ptniger.hris.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * In-Memory Smart Cache dengan Time-To-Live (TTL).
 * Menjaga aplikasi tetap SUPER RINGAN, hemat baterai, dan responsif (tanpa lag)
 * bahkan saat ribuan karyawan mengakses data serentak di jam sibuk.
 */
object SmartCacheManager {

    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val ttlMs: Long
    ) {
        fun isExpired(): Boolean = (System.currentTimeMillis() - timestamp) > ttlMs
    }

    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()

    /**
     * Mengambil data dari cache jika masih valid, atau mengeksekusi fetcher suspend jika expired/kosong.
     * @param key Kunci unik cache (misal: "all_employees", "office_locations")
     * @param ttlMs Masa berlaku cache (default: 60 detik)
     * @param fetcher Fungsi untuk mengambil data segar jika cache expired
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> getOrFetch(
        key: String,
        ttlMs: Long = 60_000L,
        fetcher: suspend () -> T
    ): T {
        val entry = cache[key] as? CacheEntry<T>
        if (entry != null && !entry.isExpired()) {
            return entry.data
        }

        val freshData = fetcher()
        if (freshData != null) {
            cache[key] = CacheEntry(
                data = freshData,
                timestamp = System.currentTimeMillis(),
                ttlMs = ttlMs
            )
        }
        return freshData
    }

    /**
     * Memaksa invalidasi cache untuk kunci tertentu (misal setelah add/edit karyawan).
     */
    fun invalidate(key: String) {
        cache.remove(key)
    }

    /**
     * Menghapus seluruh cache (misal saat logout).
     */
    fun clearAll() {
        cache.clear()
    }
}
