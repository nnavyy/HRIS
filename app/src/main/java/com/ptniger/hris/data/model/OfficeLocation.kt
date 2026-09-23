package com.ptniger.hris.data.model

data class OfficeLocation(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val allowedRadiusMeters: Double = 100.0,
    val isActive: Boolean = true,

    // Company Profile
    val companyName: String = "",
    val companyAddress: String = "",
    val companyPhone: String = "",
    val companyEmail: String = "",
    val companyNpwp: String = "",
    val companyLogoUrl: String = "",

    // Office Profile Detail (PRES-09)
    val address: String = "",               // alamat lengkap kantor
    val city: String = "",                  // "Semarang", "Ungaran", dll
    val province: String = "",              // "Jawa Tengah"
    val floor: String = "",                 // "Lantai 5", "Gedung A"
    val phone: String = "",                 // nomor telepon kantor
    val operationalHours: String = "",      // "08:00 - 17:00"
    val timeZone: String = "Asia/Jakarta",  // "Asia/Jakarta" (WIB), "Asia/Makassar" (WITA), "Asia/Jayapura" (WIT)
    val facilities: List<String> = emptyList(), // ["Parkir", "Kantin", "Gym", "Prayer Room"]
    val wifiName: String = "",              // nama WiFi kantor (info untuk karyawan)
    val maxCapacity: Int = 0,               // kapasitas maksimal karyawan di kantor ini

    // Real-time presence count (cache, di-update saat absensi)
    val presentTodayCount: Int = 0,
    val presentTodayUpdatedAt: Long = 0
)
