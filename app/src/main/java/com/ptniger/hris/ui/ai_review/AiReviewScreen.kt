package com.ptniger.hris.ui.ai_review

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ptniger.hris.data.model.AiReview
import com.ptniger.hris.data.model.Employee
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.EmployeeRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.Constants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiReviewScreen(
    user: User,
    onBack: () -> Unit,
    vm: AiReviewViewModel = viewModel()
) {
    val scope = rememberCoroutineScope()
    val state by vm.state.collectAsState()

    var employees by remember { mutableStateOf<List<Employee>>(emptyList()) }
    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var selectedPeriod by remember { mutableStateOf(getCurrentQuarter()) }
    var expandedEmp by remember { mutableStateOf(false) }
    var expandedPeriod by remember { mutableStateOf(false) }
    var expandedHistory by remember { mutableStateOf(false) }
    var isLoadingEmps by remember { mutableStateOf(true) }

    val periodOptions = generatePeriodOptions()

    val effectiveRole = user.primaryRole.ifEmpty { user.role }
    val isHR = effectiveRole == Constants.Role.HR
    val isSuperAdmin = effectiveRole == Constants.Role.SUPER_ADMIN

    LaunchedEffect(Unit) {
        val repo = EmployeeRepository()
        employees = repo.getAll()
        isLoadingEmps = false
    }

    LaunchedEffect(selectedEmployee) {
        selectedEmployee?.let {
            vm.loadHistory(it.userId.ifEmpty { it.employeeId })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Column(Modifier.padding(start = 4.dp)) {
                Text("AI Performance Review", style = MaterialTheme.typography.titleLarge)
                Text("Powered by Groq · Llama 3.3", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }

        Column(Modifier.padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // -- Employee selector --
            Text("Pilih Karyawan", style = MaterialTheme.typography.titleSmall)
            if (isLoadingEmps) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            } else {
                ExposedDropdownMenuBox(expanded = expandedEmp, onExpandedChange = { expandedEmp = it }) {
                    OutlinedTextField(
                        value = selectedEmployee?.name ?: "Pilih karyawan...",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(16.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEmp) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Blue) }
                    )
                    ExposedDropdownMenu(expanded = expandedEmp, onDismissRequest = { expandedEmp = false }) {
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text("${emp.name} · ${emp.position}") },
                                onClick = { selectedEmployee = emp; expandedEmp = false }
                            )
                        }
                    }
                }
            }

            // -- Period selector --
            Text("Periode", style = MaterialTheme.typography.titleSmall)
            ExposedDropdownMenuBox(expanded = expandedPeriod, onExpandedChange = { expandedPeriod = it }) {
                OutlinedTextField(
                    value = selectedPeriod,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPeriod) },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = Purple) }
                )
                ExposedDropdownMenu(expanded = expandedPeriod, onDismissRequest = { expandedPeriod = false }) {
                    periodOptions.forEach { p ->
                        DropdownMenuItem(text = { Text(p) }, onClick = { selectedPeriod = p; expandedPeriod = false })
                    }
                }
            }

            // -- Generate button --
            Button(
                onClick = {
                    val emp = selectedEmployee
                    if (emp == null) { return@Button }
                    vm.generateOnDemand(emp, selectedPeriod, user.uid)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !state.isGenerating && selectedEmployee != null,
                colors = ButtonDefaults.buttonColors(containerColor = Purple)
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Sedang generate review AI...")
                } else {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Review AI")
                }
            }

            // -- Error --
            state.error?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = RedSoft, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Red, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Red)
                    }
                }
            }

            // -- Message --
            state.message?.let {
                Surface(shape = RoundedCornerShape(12.dp), color = GreenSoft, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = Green)
                    }
                }
            }

            // -- Review Card --
            AnimatedVisibility(visible = state.review != null) {
                val review = state.review ?: return@AnimatedVisibility
                ReviewCard(
                    review = review,
                    isHR = isHR || isSuperAdmin,
                    onPublish = { vm.publishReview(review.reviewId) }
                )
            }

            // -- History section --
            if (state.history.isNotEmpty()) {
                HorizontalDivider(color = CardBorder)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Riwayat Review (${state.history.size})", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { expandedHistory = !expandedHistory }) {
                        Icon(
                            if (expandedHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, tint = TextSecondary
                        )
                    }
                }
                AnimatedVisibility(visible = expandedHistory) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.history.take(5).forEach { historyReview ->
                            Surface(shape = RoundedCornerShape(16.dp), color = Surface, shadowElevation = 1.dp) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(historyReview.period, style = MaterialTheme.typography.titleSmall)
                                        StatusChip(historyReview.status)
                                    }
                                    Text(
                                        historyReview.attendanceSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        "KPI: ${"%.1f".format(historyReview.kpiScoreSummary)}/100",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ReviewCard(review: AiReview, isHR: Boolean, onPublish: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Review ${review.period}", style = MaterialTheme.typography.titleMedium)
                    Text(review.attendanceSummary, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                StatusChip(review.status)
            }
            HorizontalDivider(color = CardBorder)

            // Parse 4 sections from reviewText
            Text(review.reviewText, style = MaterialTheme.typography.bodySmall)

            if (isHR && review.status == "draft" && review.reviewId.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onPublish,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)
                    ) {
                        Icon(Icons.Default.Publish, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Publish ke HR Records")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (color, label) = when (status) {
        "published" -> Green to "Published"
        else -> Orange to "Draft"
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun getCurrentQuarter(): String {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val quarter = (cal.get(Calendar.MONTH) / 3) + 1
    return "$year-Q$quarter"
}

private fun generatePeriodOptions(): List<String> {
    val cal = Calendar.getInstance()
    val currentYear = cal.get(Calendar.YEAR)
    val options = mutableListOf<String>()
    for (year in currentYear downTo (currentYear - 1)) {
        for (q in 4 downTo 1) {
            options.add("$year-Q$q")
        }
    }
    return options
}
