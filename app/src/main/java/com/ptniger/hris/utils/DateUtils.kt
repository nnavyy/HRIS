package com.ptniger.hris.utils

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
}
