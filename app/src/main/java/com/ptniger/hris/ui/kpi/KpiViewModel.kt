package com.ptniger.hris.ui.kpi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.KpiConfig
import com.ptniger.hris.data.model.KpiScore
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.data.repository.KpiRepository
import com.ptniger.hris.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class KpiViewModel : ViewModel() {
    private val kpiRepo = KpiRepository()
    private val empRepo = EmployeeRepository()
    private val _configs = MutableStateFlow<List<KpiConfig>>(emptyList())
    val configs: StateFlow<List<KpiConfig>> = _configs
    private val _scores = MutableStateFlow<List<KpiScore>>(emptyList())
    val scores: StateFlow<List<KpiScore>> = _scores
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees
    private val _totalScore = MutableStateFlow(0.0)
    val totalScore: StateFlow<Double> = _totalScore
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadConfigs() { viewModelScope.launch { _configs.value = kpiRepo.getAllConfigs() } }
    fun loadEmployees() { viewModelScope.launch { _employees.value = empRepo.getAll() } }

    fun addConfig(config: KpiConfig) {
        viewModelScope.launch {
            kpiRepo.addConfig(config).fold(
                onSuccess = { _message.value = "KPI ditambahkan"; loadConfigs() },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun deleteConfig(id: String) {
        viewModelScope.launch { kpiRepo.deleteConfig(id); loadConfigs() }
    }

    fun submitScore(score: KpiScore) {
        viewModelScope.launch {
            kpiRepo.updateScore(
                employeeId = score.employeeId,
                employeeName = score.employeeName,
                configId = score.configId,
                kpiName = score.kpiName,
                score = score.score,
                weight = score.weight,
                period = score.period,
                scoredBy = score.scoredBy,
                source = "manual",
                autoDetails = ""
            ).fold(
                onSuccess = { _message.value = "Skor disimpan"; loadScores(score.employeeId) },
                onFailure = { _message.value = "Error: ${it.message}" }
            )
        }
    }

    fun loadScores(employeeId: String) {
        viewModelScope.launch {
            val period = DateUtils.currentPeriod()
            _scores.value = kpiRepo.getScoresByEmployee(employeeId, period)
            _totalScore.value = kpiRepo.getTotalWeightedScore(employeeId, period)
        }
    }

    fun clearMessage() { _message.value = null }
}
