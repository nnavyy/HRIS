package com.ptniger.hris.ui.ai_review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.AiReview
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.repository.AiReviewRepository
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

    /**
     * Generate AI review on demand for a specific employee and period.
     * Fetches all data sources, calls AiReviewEngine, and saves to Firestore.
     */
    fun generateOnDemand(employee: Employee, period: String, generatedByUserId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isGenerating = true, error = null, message = null)
            try {
                val employeeId = employee.userId.ifEmpty { employee.employeeId }

                // Derive month/year from period (e.g., "2025-Q2" → use current month as approximation)
                val cal = Calendar.getInstance()
                val month = cal.get(Calendar.MONTH) + 1
                val year = cal.get(Calendar.YEAR)
                val monthStr = String.format("%04d-%02d", year, month)

                // Parallel fetch of all data sources
                val attendanceList = attendanceRepo.getMonthlyAttendance(employeeId, month, year)
                val kpiScores = kpiRepo.getScoresByEmployee(employeeId, period)
                val leaveHistory = leaveRepo.getByEmployee(employeeId)
                val peerReviews = peerRepo.getByTarget(employeeId, monthStr)

                val result = AiReviewEngine.generatePerformanceReview(
                    employee = employee,
                    period = period,
                    attendanceList = attendanceList,
                    kpiScores = kpiScores,
                    leaveHistory = leaveHistory,
                    peerReviews = peerReviews,
                    generatedBy = generatedByUserId,
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
