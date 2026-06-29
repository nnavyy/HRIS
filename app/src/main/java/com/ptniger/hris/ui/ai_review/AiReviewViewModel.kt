package com.ptniger.hris.ui.ai_review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.AiReview
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.repository.AiReviewRepository
import com.ptniger.hris.data.repository.AppConfigRepository
import com.ptniger.hris.data.repository.AttendanceRepository
import com.ptniger.hris.data.repository.KpiRepository
import com.ptniger.hris.data.repository.LeaveRepository
import com.ptniger.hris.data.repository.PeerReviewRepository
import com.ptniger.hris.utils.AiReviewEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class AiReviewUiState(
    val isGenerating: Boolean = false,
    val review: AiReview? = null,
    val error: String? = null,
    val history: List<AiReview> = emptyList(),
    val message: String? = null
)

class AiReviewViewModel : ViewModel() {

    private val _state = MutableStateFlow(AiReviewUiState())
    val state: StateFlow<AiReviewUiState> = _state

    private val aiRepo = AiReviewRepository()
    private val attendanceRepo = AttendanceRepository()
    private val kpiRepo = KpiRepository()
    private val leaveRepo = LeaveRepository()
    private val peerRepo = PeerReviewRepository()
    private val configRepo = AppConfigRepository()

    /**
     * Generate AI review on demand for a specific employee and period.
     * Fetches all data sources, calls AiReviewEngine, and saves to Firestore.
     */
    fun generateOnDemand(employee: Employee, period: String, generatedByUserId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGenerating = true, error = null, message = null)
            try {
                val employeeId = employee.userId.ifEmpty { employee.employeeId }

                // Parse year and quarter from period string "2025-Q2"
                val yearStr = period.substringBefore("-Q")
                val quarterNum = period.substringAfter("-Q").toIntOrNull() ?: 1
                val yearInt = yearStr.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

                // Calculate months in the quarter
                val startMonth = (quarterNum - 1) * 3 + 1   // Q1→1, Q2→4, Q3→7, Q4→10
                val endMonth = startMonth + 2                 // Q1→3, Q2→6, Q3→9, Q4→12

                // Fetch attendance for all 3 months in the quarter
                val attendanceList = (startMonth..endMonth).flatMap { m ->
                    attendanceRepo.getMonthlyAttendance(employeeId, m, yearInt)
                }

                // Fetch peer review with mid-quarter month
                val midMonth = startMonth + 1
                val periodMonthStr = String.format("%04d-%02d", yearInt, midMonth)
                val peerReviews = peerRepo.getByTarget(employeeId, periodMonthStr)

                // KPI scores use the period string directly (e.g. "2025-Q2")
                val kpiScores = kpiRepo.getScoresByEmployee(employeeId, period)
                // Leave history: fetch all for this employee
                val leaveHistory = leaveRepo.getByEmployee(employeeId)

                val apiKey = configRepo.getGroqApiKey()
                if (apiKey.isBlank()) {
                    _state.value = _state.value.copy(
                        isGenerating = false,
                        error = "API Key Groq belum di-set. Hubungi Super Admin."
                    )
                    return@launch
                }

                val result = AiReviewEngine.generatePerformanceReview(
                    employee = employee,
                    period = period,
                    attendanceList = attendanceList,
                    kpiScores = kpiScores,
                    leaveHistory = leaveHistory,
                    peerReviews = peerReviews,
                    generatedBy = generatedByUserId,
                    apiKey = apiKey,
                    triggerType = "on_demand"
                )

                result.fold(
                    onSuccess = { review ->
                        aiRepo.save(review).fold(
                            onSuccess = { savedId ->
                                val savedReview = review.copy(reviewId = savedId)
                                _state.value = _state.value.copy(
                                    isGenerating = false,
                                    review = savedReview,
                                    message = "AI Review berhasil di-generate! ✅"
                                )
                                loadHistory(employeeId)
                            },
                            onFailure = { e ->
                                _state.value = _state.value.copy(
                                    isGenerating = false,
                                    error = "Gagal menyimpan review: ${e.message}"
                                )
                            }
                        )
                    },
                    onFailure = { e ->
                        _state.value = _state.value.copy(
                            isGenerating = false,
                            error = "Gagal generate AI review: ${e.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isGenerating = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun loadHistory(employeeId: String) {
        viewModelScope.launch {
            val history = aiRepo.getByEmployee(employeeId)
            _state.value = _state.value.copy(history = history)
        }
    }

    fun publishReview(reviewId: String) {
        viewModelScope.launch {
            aiRepo.publish(reviewId).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        review = _state.value.review?.copy(status = "published"),
                        message = "Review berhasil dipublikasikan."
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(error = "Gagal publish: ${e.message}")
                }
            )
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }
}
