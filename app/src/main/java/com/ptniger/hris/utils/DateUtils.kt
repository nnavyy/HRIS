package com.ptniger.hris.utils

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    private val periodFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    fun today(): String = dateFormat.format(Date())
    fun nowTime(): String = timeFormat.format(Date())
    fun currentPeriod(): String = periodFormat.format(Date())
    fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)

    fun formatDate(dateStr: String): String {
        return try {
            val date = dateFormat.parse(dateStr)
            date?.let { displayDateFormat.format(it) } ?: dateStr
        } catch (e: Exception) { dateStr }
    }

    fun formatMonthYear(month: Int, year: Int): String {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.YEAR, year)
        return monthYearFormat.format(cal.time)
    }

    fun isLate(checkInTime: String, threshold: String = "08:15"): Boolean {
        return checkInTime > threshold
    }

    fun calculateLateMinutes(checkInTime: String, threshold: String = "08:00"): Int {
        return try {
            val checkIn = timeFormat.parse(checkInTime)
            val limit = timeFormat.parse(threshold)
            if (checkIn != null && limit != null && checkIn.after(limit)) {
                ((checkIn.time - limit.time) / 60000).toInt()
            } else 0
        } catch (e: Exception) { 0 }
    }

    fun calculateOvertimeHours(checkOutTime: String, threshold: String = "17:00"): Double {
        return try {
            val checkOut = timeFormat.parse(checkOutTime)
            val limit = timeFormat.parse(threshold)
            if (checkOut != null && limit != null && checkOut.after(limit)) {
                val diffMinutes = (checkOut.time - limit.time) / 60000
                // Convert minutes to hours. E.g., 90 minutes = 1.5 hours
                Math.round((diffMinutes / 60.0) * 10.0) / 10.0
            } else 0.0
        } catch (e: Exception) { 0.0 }
    }

    /**
     * Gets approximate server time by writing a temporary doc with serverTimestamp
     * and reading back the result. Returns server epoch millis.
     * Falls back to local time if fetch fails.
     */
    suspend fun getServerTimeMillis(): Long {
        return try {
            val db = FirebaseFirestore.getInstance()
            val tempRef = db.collection("_server_time_check").document("probe")
            tempRef.set(mapOf("ts" to com.google.firebase.firestore.FieldValue.serverTimestamp())).await()
            val doc = tempRef.get(com.google.firebase.firestore.Source.SERVER).await()
            val serverTs = doc.getTimestamp("ts")
            tempRef.delete().await() // cleanup
            serverTs?.toDate()?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    /**
     * Returns formatted time string from server time.
     */
    suspend fun serverNowTime(): String {
        val serverMillis = getServerTimeMillis()
        return timeFormat.format(Date(serverMillis))
    }

    /**
     * Returns formatted date string from server time.
     */
    suspend fun serverToday(): String {
        val serverMillis = getServerTimeMillis()
        return dateFormat.format(Date(serverMillis))
    }

    /**
     * Checks if there's a significant time difference between device and server.
     * Returns the offset in milliseconds.
     * Positive means device is ahead of server. Negative means device is behind.
     */
    suspend fun getDeviceServerOffsetMs(): Long {
        val deviceTime = System.currentTimeMillis()
        val serverTime = getServerTimeMillis()
        return deviceTime - serverTime
    }

    /**
     * Checks if device time has been tampered with (offset > 2 minutes).
     */
    suspend fun isTimeTampered(thresholdMs: Long = 2 * 60 * 1000): Boolean {
        val offset = Math.abs(getDeviceServerOffsetMs())
        return offset > thresholdMs
    }
}
