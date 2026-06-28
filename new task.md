# HRIS Enhancement — AI Agent Task List
> Project: `com.ptniger.hris` (Kotlin + Jetpack Compose + Firebase)
> Generated for: Nanda Zhafran Mahendra
> Stack tambahan: Groq API (Llama-3, free tier) untuk AI Review

---

## PHASE 1 — PAYROLL OVERHAUL

### TASK-P01 · Model `EmployeeContract` (data class baru)
**File baru:** `data/model/EmployeeContract.kt`

Buat data class dengan field:
- `contractId: String`
- `employeeId: String`
- `effectiveDate: String` (format: `YYYY-MM-DD`)
- `baseSalary: Double`
- `allowanceMeal: Double` — tunjangan makan
- `allowanceTransport: Double` — tunjangan transport
- `allowancePosition: Double` — tunjangan jabatan
- `bpjsJkkRate: Double` — default `0.0024` (0.24%, risiko kerja 1 sesuai PP 44/2015)
- `bpjsJkmRate: Double` — default `0.003` (0.3%)
- `ptkpStatus: String` — TK/0, TK/1, K/0, K/1, K/2, K/3 (untuk PPh 21)
- `overrideOvertimeMultiplier: Boolean` — jika true, pakai nilai di bawah
- `overtime1xMultiplier: Double` — default `1.5`
- `overtimeNxMultiplier: Double` — default `2.0`
- `signedByEmployee: Boolean` — default `false`
- `signedAt: Long`
- `signatureData: String` — base64 string dari tanda tangan digital
- `createdBy: String` — userId HR yang buat
- `createdAt: Long`

**Firestore collection:** `employee_contracts`

---

### TASK-P02 · `ContractRepository.kt`
**File baru:** `data/repository/ContractRepository.kt`

Fungsi yang perlu dibuat:
```
suspend fun create(contract: EmployeeContract): Result<String>
suspend fun getActiveContract(employeeId: String): EmployeeContract?
suspend fun getContractHistory(employeeId: String): List<EmployeeContract>
suspend fun signContract(contractId: String, signatureBase64: String): Result<Unit>
suspend fun getUnsignedContracts(employeeId: String): List<EmployeeContract>
```

Logic `getActiveContract`: ambil kontrak dengan `effectiveDate` terbaru yang `<= today` dan `signedByEmployee == true`.

---

### TASK-P03 · `PayrollCalculator.kt` — Overhaul Total
**File existing:** `utils/PayrollCalculator.kt`

Tambahkan / ubah fungsi berikut:

#### 3a. Tunjangan
```kotlin
fun calculateTotalAllowance(
    allowanceMeal: Double,
    allowanceTransport: Double,
    allowancePosition: Double
): Double
```

#### 3b. BPJS JKK & JKM (dari perusahaan)
```kotlin
// PP 44/2015 — ditanggung perusahaan, bukan potongan karyawan
fun calculateBpjsJkk(baseSalary: Double, jkkRate: Double): Double
fun calculateBpjsJkm(baseSalary: Double, jkmRate: Double = 0.003): Double
```

#### 3c. PPh 21 — Metode TER (PMK 168/2023, berlaku Jan 2024)
```kotlin
fun calculatePph21Ter(
    grossMonthly: Double,  // gaji bruto sebulan
    ptkpStatus: String     // TK/0, TK/1, K/0, K/1, K/2, K/3
): Double
```

Implementasi TER (Tarif Efektif Rata-rata):
- Cari kategori TER berdasarkan `ptkpStatus`:
  - `TK/0` → Kategori A
  - `TK/1`, `TK/2`, `TK/3`, `K/0` → Kategori B
  - `K/1`, `K/2`, `K/3` → Kategori C
- Lookup tabel tarif bulanan dari PMK 168/2023 (hardcode sebagai map `grossRange → tariffPercent`)
- Return `grossMonthly * tariffPercent`

> Referensi tabel TER: https://www.pajak.go.id/pmk-168-2023

#### 3d. Net Salary final
```kotlin
fun calculateFullNetSalary(
    baseSalary: Double,
    allowanceMeal: Double,
    allowanceTransport: Double,
    allowancePosition: Double,
    overtimePay: Double,
    kpiBonus: Double,
    bpjsKes: Double,       // existing, potongan karyawan
    bpjsJht: Double,       // existing, potongan karyawan
    bpjsJp: Double,        // existing, potongan karyawan
    pph21: Double,
    otherDeductions: Double
): Double
// gross = baseSalary + semua tunjangan + overtime + kpiBonus
// net = gross - (bpjsKes + bpjsJht + bpjsJp + pph21 + otherDeductions)
```

---

### TASK-P04 · `PayrollRepository.generate()` — Update
**File existing:** `data/repository/PayrollRepository.kt`

Update fungsi `generate()`:
1. Panggil `ContractRepository().getActiveContract(employeeId)` untuk ambil contract aktif
2. Kalau contract ada → pakai nilai tunjangan & rate dari contract
3. Kalau contract tidak ada → pakai nilai default / parameter yang dipassin
4. Hitung `bpjsJkk` dan `bpjsJkm` (simpan ke Payroll model sebagai info, bukan potongan karyawan)
5. Hitung `pph21` via `PayrollCalculator.calculatePph21Ter()`
6. Update `Payroll` data class: tambah field `allowanceMeal`, `allowanceTransport`, `allowancePosition`, `bpjsJkk`, `bpjsJkm`, `pph21`, `ptkpStatus`, `contractId`

---

### TASK-P05 · UI — `ContractFormScreen.kt` (baru)
**File baru:** `ui/contract/ContractFormScreen.kt`

Screen untuk HR buat/edit kontrak karyawan:
- Form input semua field dari `EmployeeContract`
- Dropdown `ptkpStatus` (TK/0 s/d K/3)
- Toggle `overrideOvertimeMultiplier`
- Preview kalkulasi payroll berdasarkan nilai yang diinput (real-time)
- Tombol "Simpan & Kirim ke Karyawan untuk TTD"

**Akses:** HR & Super Admin only

---

### TASK-P06 · UI — `ContractSignScreen.kt` (baru)
**File baru:** `ui/contract/ContractSignScreen.kt`

Screen untuk karyawan tanda tangan kontrak:
- Tampilkan detail kontrak (read-only)
- Canvas untuk tanda tangan digital (pakai `androidx.compose.ui.graphics.Canvas` atau library `signature-pad`)
- Tombol "Setuju & Tanda Tangan"
- Setelah sign → panggil `ContractRepository.signContract()` dengan base64 tanda tangan
- Tampilkan badge "Sudah Ditandatangani" jika sudah

**Akses:** Employee yang punya kontrak pending

---

### TASK-P07 · Notifikasi kontrak pending
**File existing:** `data/repository/NotificationRepository.kt`

Tambahkan trigger notifikasi:
- Saat HR buat kontrak baru → kirim notif ke karyawan terkait: `"Ada kontrak baru yang perlu ditandatangani"`
- Notif muncul di `NotificationScreen` yang sudah ada

---

## PHASE 2 — KPI OVERHAUL

### TASK-K01 · Model `KpiConfig.kt` — Update
**File existing:** `data/model/KpiConfig.kt`

Tambahkan dimensi penilaian baru ke enum/konstanta:
```kotlin
object KpiDimension {
    const val PERFORMANCE = "performance"       // existing
    const val ATTENDANCE = "attendance"         // existing
    const val DISCIPLINE = "discipline"         // existing
    const val WORKLOAD = "workload"             // existing
    const val OUTPUT_QUALITY = "output_quality" // NEW
    const val PEER_REVIEW = "peer_review"       // NEW
    const val GOAL_ACHIEVEMENT = "goal_achievement" // NEW
    const val TEAM_CONTRIBUTION = "team_contribution" // NEW
}
```

Tambahkan field ke `KpiConfig`:
- `dimension: String` — pakai konstanta di atas
- `goalTarget: Double?` — untuk tipe `GOAL_ACHIEVEMENT` (target angka)
- `goalActual: Double?` — realisasi
- `reviewers: List<String>` — untuk tipe `PEER_REVIEW` (list userId reviewer)

---

### TASK-K02 · Model `PeerReview.kt` (baru)
**File baru:** `data/model/PeerReview.kt`

```kotlin
data class PeerReview(
    val reviewId: String = "",
    val targetEmployeeId: String = "",
    val reviewerEmployeeId: String = "",
    val period: String = "",          // format: "YYYY-MM"
    val score: Int = 0,               // 0–100
    val comments: String = "",
    val dimension: String = "",       // KpiDimension yang direview
    val createdAt: Long = System.currentTimeMillis()
)
```

**Firestore collection:** `peer_reviews`

---

### TASK-K03 · `PeerReviewRepository.kt` (baru)
**File baru:** `data/repository/PeerReviewRepository.kt`

```
suspend fun submit(review: PeerReview): Result<String>
suspend fun getByTarget(employeeId: String, period: String): List<PeerReview>
suspend fun getAverageScore(employeeId: String, period: String): Double
suspend fun hasPeerReviewed(reviewerId: String, targetId: String, period: String): Boolean
```

---

### TASK-K04 · `KpiCalculator.kt` — Update
**File existing:** `utils/KpiCalculator.kt`

Tambahkan fungsi:

```kotlin
// Hitung goal achievement score: (actual / target) * 100, capped 100
fun calculateGoalScore(target: Double, actual: Double): Int

// Hitung attendance KPI score berdasarkan data absensi bulan tsb
fun calculateAttendanceScore(
    totalWorkDays: Int,
    presentDays: Int,
    lateDays: Int,
    absentDays: Int
): Int // formula: (presentDays / totalWorkDays * 70) + (1 - lateRate) * 20 + (1 - absentRate) * 10

// Hitung discipline score berdasarkan audit log & warning
fun calculateDisciplineScore(warningCount: Int, violationCount: Int): Int

// Aggregate semua dimensi jadi 1 weighted score
fun calculateAggregateKpiScore(
    scores: Map<String, Int>,    // dimension -> score
    weights: Map<String, Double> // dimension -> weight (total harus = 1.0)
): Double
```

---

### TASK-K05 · UI — `PeerReviewScreen.kt` (baru)
**File baru:** `ui/kpi/PeerReviewScreen.kt`

- List karyawan yang perlu direview (berdasarkan `reviewers` di KpiConfig)
- Form scoring per dimensi (slider 0–100)
- Field komentar
- Submit → `PeerReviewRepository.submit()`
- Cek duplikasi: kalau sudah pernah review periode ini, tampilkan hasil sebelumnya (read-only)

**Akses:** Semua role (Employee, Manager, HR)

---

## PHASE 3 — AI PERFORMANCE REVIEW

### TASK-A01 · Setup Groq API Client
**File baru:** `utils/GroqAiClient.kt`

```kotlin
object GroqAiClient {
    private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
    private const val MODEL = "llama-3.3-70b-versatile" // gratis, tercepat
    
    // API key disimpan di local.properties atau BuildConfig (JANGAN hardcode)
    // Baca via: BuildConfig.GROQ_API_KEY
    
    suspend fun generateReview(prompt: String): Result<String>
    // Pakai OkHttp (sudah ada di dependencies)
    // Request: POST ke BASE_URL dengan Authorization: Bearer {key}
    // Response: ambil choices[0].message.content
}
```

**Catatan keamanan:** API key Groq wajib disimpan di `local.properties`:
```
GROQ_API_KEY=gsk_xxxxx
```
Dan di `build.gradle.kts`:
```kotlin
buildConfigField("String", "GROQ_API_KEY", properties["GROQ_API_KEY"].toString())
```

---

### TASK-A02 · `AiReviewEngine.kt` (baru)
**File baru:** `utils/AiReviewEngine.kt`

Engine yang kumpulkan semua data karyawan dan bangun prompt:

```kotlin
object AiReviewEngine {

    suspend fun buildEmployeeContext(
        employee: Employee,
        period: String, // "YYYY-Q1" / "YYYY-Q2" dst
        attendanceList: List<Attendance>,
        kpiScores: List<KpiScore>,
        leaveHistory: List<LeaveRequest>,
        peerReviews: List<PeerReview>,
        payroll: Payroll?
    ): String
    // Return: context string berisi summary data dalam Bahasa Indonesia
    
    suspend fun generatePerformanceReview(
        employee: Employee,
        period: String,
        // ... semua data sama seperti buildEmployeeContext
    ): Result<AiReview>
    
    fun buildPrompt(context: String, employeeName: String, period: String): String
    // Prompt harus mencakup:
    // - Instruksi bahasa Indonesia
    // - Tone: profesional, objektif, konstruktif
    // - Output format: 4 seksi (Ringkasan Kinerja, Kekuatan, Area Pengembangan, Rekomendasi)
    // - Batas: maks 400 kata
    // - JANGAN sebut angka gaji/payroll di output
}
```

---

### TASK-A03 · Model `AiReview.kt` (baru)
**File baru:** `data/model/AiReview.kt`

```kotlin
data class AiReview(
    val reviewId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val period: String = "",           // "2025-Q1"
    val generatedAt: Long = System.currentTimeMillis(),
    val generatedBy: String = "",      // userId HR/Manager yang trigger (atau "system" kalau scheduled)
    val triggerType: String = "",      // "on_demand" | "scheduled"
    val reviewText: String = "",       // output dari Groq
    val modelUsed: String = "llama-3.3-70b-versatile",
    val kpiScoreSummary: Double = 0.0,
    val attendanceSummary: String = "", // "Hadir: 20/22 hari, Terlambat: 2x"
    val status: String = "draft"       // "draft" | "published"
)
```

**Firestore collection:** `ai_reviews`

**Firestore Rules — tambahkan:**
```
match /ai_reviews/{reviewId} {
  allow read: if isSuperAdmin() || isHR() || isManager();
  allow create: if isSuperAdmin() || isHR() || isManager();
  allow update, delete: if isSuperAdmin() || isHR();
}
```

---

### TASK-A04 · `AiReviewRepository.kt` (baru)
**File baru:** `data/repository/AiReviewRepository.kt`

```
suspend fun save(review: AiReview): Result<String>
suspend fun getByEmployee(employeeId: String): List<AiReview>
suspend fun getByPeriod(period: String): List<AiReview>
suspend fun publish(reviewId: String): Result<Unit>   // ubah status jadi "published"
suspend fun getLatest(employeeId: String): AiReview?
```

---

### TASK-A05 · `AiReviewViewModel.kt` (baru)
**File baru:** `ui/ai_review/AiReviewViewModel.kt`

State:
```kotlin
data class AiReviewUiState(
    val isGenerating: Boolean = false,
    val review: AiReview? = null,
    val error: String? = null,
    val history: List<AiReview> = emptyList()
)
```

Fungsi:
```kotlin
fun generateOnDemand(employeeId: String, period: String)
// 1. Fetch semua data karyawan (attendance, kpi, leave, peer review, payroll)
// 2. Panggil AiReviewEngine.generatePerformanceReview()
// 3. Save ke Firestore via AiReviewRepository
// 4. Update state

fun loadHistory(employeeId: String)
fun publishReview(reviewId: String)
```

---

### TASK-A06 · UI — `AiReviewScreen.kt` (baru)
**File baru:** `ui/ai_review/AiReviewScreen.kt`

Layout:
1. **Header**: Nama karyawan + periode selector (dropdown Q1–Q4 + tahun)
2. **Generate button**: "Generate Review AI" → loading state dengan animasi (pakai `CircularProgressIndicator`)
3. **Review card**: Tampilkan `reviewText` dalam card, dengan 4 seksi yang diparse dari output AI
4. **Status chip**: Draft / Published
5. **Action buttons**: "Publish" (HR only) | "Regenerate"
6. **History section**: Accordion list review sebelumnya

**Akses:** HR & Manager only

---

### TASK-A07 · Scheduled Review — AutomationEngine Extension
**File existing:** `utils/AutomationEngine.kt`

Tambahkan rule type baru:
```kotlin
object RuleType {
    // ... existing rules ...
    const val AI_REVIEW = "ai_review"  // NEW
}
```

**File baru:** `utils/QuarterlyReviewScheduler.kt`

```kotlin
object QuarterlyReviewScheduler {
    // Cek apakah sekarang adalah akhir kuartal (Maret/Juni/Sept/Des)
    fun isEndOfQuarter(): Boolean
    
    // Dipanggil saat app dibuka (di LoginViewModel.checkExistingSession)
    // Jika isEndOfQuarter() && belum ada ai_review untuk periode ini && rule AI_REVIEW aktif
    // → trigger generateForAllEmployees()
    suspend fun checkAndTriggerIfNeeded(employees: List<Employee>)
    
    // Generate review untuk semua karyawan aktif
    // Jalankan sequential (bukan parallel) untuk hindari rate limit Groq
    suspend fun generateForAllEmployees(
        employees: List<Employee>,
        period: String
    )
    
    fun getCurrentQuarterPeriod(): String // return "2025-Q2"
}
```

---

## PHASE 4 — SECURITY FIXES (dari analisis codebase)

### TASK-S01 · Fix Firestore Rule — Employee Update
**File:** `firestore.rules`

Ubah rule `/employees`:
```
// SEBELUM (bug: semua login user bisa update employee manapun)
allow update: if isSuperAdmin() || isHR() || isManager() || isLoggedIn();

// SESUDAH
allow update: if isSuperAdmin() || isHR() || isManager() 
              || (isLoggedIn() && request.auth.uid == resource.data.userId);
```

---

### TASK-S02 · Fix Storage Rules — Multi-role Support
**File:** `storage.rules`

Update fungsi `getUserRole()` agar support multi-role (`roles` list), bukan hanya field `role` tunggal:
```javascript
function hasAnyRole(roleList) {
  let userData = firestore.get(/databases/(default)/documents/users/$(request.auth.uid)).data;
  return userData.primaryRole in roleList 
      || userData.role in roleList
      || (userData.roles != null && userData.roles.hasAny(roleList));
}
```

---

### TASK-S03 · Cloudinary Upload — Mitigasi Unsigned Preset
**File:** `data/repository/AttendanceRepository.kt`

Tambahkan validasi sebelum upload:
- Cek file size < 3MB sebelum upload (bukan hanya di Storage rules)
- Tambahkan parameter `folder: "hris_attendance/{employeeId}"` di request body agar upload terorganisir
- Tambahkan `context: "employeeId={uid}|date={today}"` sebagai metadata Cloudinary

**Catatan untuk kamu:** Di Cloudinary dashboard, set upload preset `hris_upload` agar hanya terima `image/*` dan max size 3MB. Ini tidak bisa dikontrol dari kode, harus dari dashboard.

---

### TASK-S04 · Groq API Key — Build Config Setup
**File:** `app/build.gradle.kts`

Tambahkan:
```kotlin
val localProperties = java.util.Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) localProperties.load(localFile.inputStream())

android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField(
            "String", "GROQ_API_KEY",
            "\"${localProperties["GROQ_API_KEY"] ?: ""}\""
        )
    }
}
```

---

## PHASE 5 — NAVIGATION & INTEGRATION

### TASK-N01 · Tambah route baru ke `AppNavigation.kt`
**File existing:** `ui/navigation/AppNavigation.kt`

Tambahkan case baru di `when (currentRoute)`:
```kotlin
"contract_form" -> ContractFormScreen(user = user, onBack = { onNavigate("employees") })
"contract_sign" -> ContractSignScreen(user = user, onBack = { onNavigate("dashboard") })
"peer_review" -> PeerReviewScreen(user = user, onBack = { onNavigate("kpi_result") })
"ai_review" -> AiReviewScreen(user = user, onBack = { onNavigate("dashboard") })
```

---

### TASK-N02 · Update `RoleManager.kt` — Nav items baru
**File existing:** `utils/RoleManager.kt`

- HR → tambah `NavItem("ai_review", "AI Review", "smart_toy")`
- Manager → tambah `NavItem("peer_review", "Peer Review", "rate_review")` dan `NavItem("ai_review", "AI Review", "smart_toy")`
- Employee → tambah `NavItem("contract_sign", "Kontrak", "description")` jika ada kontrak pending

---

### TASK-N03 · Update Firestore Rules — tambah collection baru
**File:** `firestore.rules`

Tambahkan:
```
match /employee_contracts/{contractId} {
  allow read: if isSuperAdmin() || isHR() || isManager()
              || (isLoggedIn() && resource.data.employeeId == request.auth.uid);
  allow create, update: if isSuperAdmin() || isHR();
  allow delete: if isSuperAdmin();
}

match /peer_reviews/{reviewId} {
  allow read: if isSuperAdmin() || isHR() || isManager();
  allow create: if isLoggedIn();
  allow update, delete: if false;
}

match /ai_reviews/{reviewId} {
  allow read: if isSuperAdmin() || isHR() || isManager();
  allow create: if isSuperAdmin() || isHR() || isManager();
  allow update: if isSuperAdmin() || isHR();
  allow delete: if isSuperAdmin();
}
```

---

## URUTAN PENGERJAAN YANG DISARANKAN

```
Phase 1 (Payroll):  P01 → P02 → P03 → P04 → P05 → P06 → P07
Phase 2 (KPI):      K01 → K02 → K03 → K04 → K05
Phase 4 (Security): S01 → S02 → S03 → S04   ← kerjain paralel sama Phase 1-2
Phase 3 (AI):       A01 → A02 → A03 → A04 → A05 → A06 → A07
Phase 5 (Nav):      N01 → N02 → N03   ← terakhir, setelah semua screen jadi
```

---

## CATATAN PENTING UNTUK AI AGENT

1. **Jangan ubah** fungsi `calculateBpjsKesehatan()`, `calculateBpjsJht()`, `calculateBpjsJp()` yang sudah ada — itu potongan karyawan. JKK & JKM yang baru adalah **tanggungan perusahaan** (tidak dipotong dari gaji karyawan).

2. **Tabel TER PPh 21** harus diimplementasi sebagai lookup table statis berdasarkan PMK 168/2023. Jangan hitung progressif manual — TER sudah flat per bracket.

3. **Groq rate limit** free tier: 30 req/menit, 14.400 req/hari. Untuk scheduled quarterly review semua karyawan, jalankan **sequential dengan delay 2 detik** antar request agar tidak kena rate limit.

4. **Peer review** tidak boleh bisa direview oleh diri sendiri — tambahkan validasi `reviewerId != targetEmployeeId`.

5. **AI Review text** tidak boleh tampil ke karyawan yang bersangkutan — pastikan Firestore rules dan UI sudah memblokir akses employee ke `ai_reviews`.

6. **Tanda tangan digital** disimpan sebagai base64 string. Untuk MVP ini acceptable, tapi bukan legally binding. Cukup disclaimer di `ContractSignScreen`.