package com.ptniger.hris.utils

import android.content.Context
import android.net.Uri
import com.ptniger.hris.data.model.Employee
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvParser {
    /**
     * Parses a CSV file from the given URI and returns a list of Employees.
     * Expected CSV Header (order matters if ignoring headers, but we will use indexing just in case, or simply split by comma):
     * nik,name,email,phone,position,department,branch,joinDate,baseSalary,leaveQuota
     */
    fun parseEmployees(context: Context, uri: Uri): List<Employee> {
        val employees = mutableListOf<Employee>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            var isFirstLine = true
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                // Skip header
                if (isFirstLine) {
                    isFirstLine = false
                    continue
                }
                
                val tokens = line?.split(",")?.map { it.trim() }
                if (tokens != null && tokens.size >= 10) {
                    val employee = Employee(
                        nik = tokens[0],
                        name = tokens[1],
                        email = tokens[2],
                        phone = tokens[3],
                        position = tokens[4],
                        department = tokens[5],
                        branch = tokens[6],
                        joinDate = tokens[7],
                        baseSalary = tokens[8].toDoubleOrNull() ?: 0.0,
                        leaveQuota = tokens[9].toIntOrNull() ?: 12,
                        employmentStatus = "active" // Default
                    )
                    employees.add(employee)
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Gagal membaca file CSV: ${e.message}")
        }
        return employees
    }
}
