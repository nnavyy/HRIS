package com.ptniger.hris.ui.admin

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.AutomationRule
import com.ptniger.hris.data.model.User
import com.ptniger.hris.data.repository.AuditLogRepository
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.Constants
import com.ptniger.hris.utils.RoleManager
import kotlinx.coroutines.launch

@Composable
fun RoleManagementScreen(user: User) {
    val roles = listOf(Constants.Role.HR, Constants.Role.FINANCE, Constants.Role.MANAGER, Constants.Role.EMPLOYEE)

    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        Text("Role & Access", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(18.dp))
        roles.forEach { role ->
            val color = when (role) { Constants.Role.HR -> Blue; Constants.Role.FINANCE -> Orange; Constants.Role.MANAGER -> Teal; else -> Pink }
            val bg = when (role) { Constants.Role.HR -> BlueSoft; Constants.Role.FINANCE -> OrangeSoft; Constants.Role.MANAGER -> TealSoft; else -> PinkSoft }
            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), shape = RoundedCornerShape(22.dp), color = Surface, shadowElevation = 1.dp) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
                        Text(RoleManager.getRoleShort(role), style = MaterialTheme.typography.labelMedium, color = color)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(RoleManager.getRoleDisplayName(role), style = MaterialTheme.typography.titleSmall)
                        Text(RoleManager.getNavItems(role).joinToString(", ") { it.label }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Surface(shape = RoundedCornerShape(999.dp), color = bg) {
                        Text("Aktif", Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun AutomationScreen(user: User) {
    val repo = remember { AuditLogRepository() }
    var rules by remember { mutableStateOf<List<AutomationRule>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        repo.seedDefaultRules()
        rules = repo.getRules()
    }

    Column(Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState())) {
        Text("Automation Rules", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(18.dp))
        rules.forEach { rule ->
            val colors = mapOf("attendance" to (TealSoft to Teal), "leave" to (BlueSoft to Blue), "payroll" to (OrangeSoft to Orange),
                "notification" to (PurpleSoft to Purple), "account" to (RedSoft to Red), "audit" to (GreenSoft to Green))
            val (bg, fg) = colors[rule.type] ?: (BlueSoft to Blue)

            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp), shape = RoundedCornerShape(22.dp), color = Surface, shadowElevation = 1.dp) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Settings, null, tint = fg, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(rule.name, style = MaterialTheme.typography.titleSmall)
                        Text(rule.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = rule.isActive,
                        onCheckedChange = { checked ->
                            scope.launch {
                                repo.toggleRule(rule.ruleId, checked)
                                rules = repo.getRules()
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = Green)
                    )
                }
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}
