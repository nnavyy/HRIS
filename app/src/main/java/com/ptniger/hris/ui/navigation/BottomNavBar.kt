package com.ptniger.hris.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ptniger.hris.ui.theme.*
import com.ptniger.hris.utils.RoleManager

@Composable
fun BottomNavBar(
    role: String,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = RoleManager.getNavItems(role)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .shadow(12.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                NavButton(
                    icon = getIcon(item.icon),
                    label = item.label,
                    selected = selected,
                    onClick = { onNavigate(item.route) }
                )
            }
        }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (selected) Modifier.background(BlueSoft)
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) Blue else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Blue else TextSecondary
        )
    }
}

private fun getIcon(name: String): ImageVector {
    return when (name) {
        "home" -> Icons.Default.Home
        "people" -> Icons.Default.People
        "calendar" -> Icons.Default.CalendarMonth
        "star" -> Icons.Default.Star
        "person" -> Icons.Default.Person
        "payments" -> Icons.Default.Payments
        "chart" -> Icons.Default.BarChart
        "shield" -> Icons.Default.Shield
        "clock" -> Icons.Default.AccessTime
        "admin" -> Icons.Default.AdminPanelSettings
        "settings" -> Icons.Default.Settings
        else -> Icons.Default.Circle
    }
}
