package com.ptniger.hris.utils

import android.content.Context
import org.json.JSONArray

object LoginHistoryManager {
    private const val PREF_NAME = "login_history_pref"
    private const val KEY_EMAILS = "emails_history"
    private const val MAX_HISTORY = 5

    fun getHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_EMAILS, null) ?: return emptyList()
        
        val list = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveHistory(context: Context, email: String) {
        if (email.isBlank()) return
        
        val currentHistory = getHistory(context).toMutableList()
        
        // Remove if it already exists to move it to the top
        currentHistory.remove(email)
        
        // Add to the top (index 0)
        currentHistory.add(0, email)
        
        // Keep only max items
        if (currentHistory.size > MAX_HISTORY) {
            currentHistory.subList(MAX_HISTORY, currentHistory.size).clear()
        }
        
        // Save back to SharedPreferences
        val jsonArray = JSONArray()
        currentHistory.forEach { jsonArray.put(it) }
        
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_EMAILS, jsonArray.toString()).apply()
    }
}
