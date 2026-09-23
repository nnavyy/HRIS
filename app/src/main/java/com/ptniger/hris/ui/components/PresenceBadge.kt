package com.ptniger.hris.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptniger.hris.data.model.PresenceStatus
import com.ptniger.hris.data.model.WorkSchedule
import com.ptniger.hris.ui.theme.*
import java.util.Calendar

/**
 * Badge status presence yang dipakai di banyak tempat:
 * - Employee card di list
 * - Header profil karyawan
 * - Team presence grid
 * - Dashboard metric
 *
 * Ukuran: DOT (hanya dot), COMPACT (dot + teks kecil), FULL (icon + teks + detail)
 */
@Composable
fun PresenceBadge(
    status: PresenceStatus,
    size: PresenceBadgeSize = PresenceBadgeSize.COMPACT,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = presenceColors(status)
    val icon = presenceIcon(status)

    when (size) {
        PresenceBadgeSize.DOT -> {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(textColor)
                    .then(modifier)
            )
        }
        PresenceBadgeSize.COMPACT -> {
            Surface(shape = RoundedCornerShape(999.dp), color = bgColor, modifier = modifier) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(icon, null, tint = textColor, modifier = Modifier.size(12.dp))
                    Text(status.label, style = MaterialTheme.typography.labelSmall, color = textColor)
                }
            }
        }
        PresenceBadgeSize.FULL -> {
            Surface(shape = RoundedCornerShape(14.dp), color = bgColor, modifier = modifier) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, null, tint = textColor, modifier = Modifier.size(20.dp))
                    Text(status.label, style = MaterialTheme.typography.labelMedium,
                        color = textColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

enum class PresenceBadgeSize { DOT, COMPACT, FULL }

/** Warna background dan foreground per status */
fun presenceColors(status: PresenceStatus): Pair<Color, Color> = when (status) {
    PresenceStatus.PRESENT_ONSITE -> GreenSoft to Green
    PresenceStatus.PRESENT_WFH   -> PurpleSoft to Purple
    PresenceStatus.LATE_ONSITE   -> OrangeSoft to Orange
    PresenceStatus.NOT_YET       -> OrangeSoft to Orange
    PresenceStatus.ON_LEAVE      -> BlueSoft to Blue
    PresenceStatus.SICK          -> RedSoft.copy(alpha = 0.5f) to Red.copy(alpha = 0.7f)
    PresenceStatus.PERMISSION    -> TealSoft to Teal
    PresenceStatus.ABSENT        -> RedSoft to Red
    PresenceStatus.OFF_DAY       -> CardBorder to TextMuted
    PresenceStatus.UNKNOWN       -> CardBorder to TextMuted
}

/** Material icon per status — hanya pakai icon dari default filled set */
fun presenceIcon(status: PresenceStatus): ImageVector = when (status) {
    PresenceStatus.PRESENT_ONSITE -> Icons.Default.LocationCity     // kantor
    PresenceStatus.PRESENT_WFH   -> Icons.Default.Home             // rumah
    PresenceStatus.LATE_ONSITE   -> Icons.Default.Schedule          // jam/terlambat
    PresenceStatus.NOT_YET       -> Icons.Default.AccessTime        // belum absen
    PresenceStatus.ON_LEAVE      -> Icons.Default.CalendarMonth     // cuti
    PresenceStatus.SICK          -> Icons.Default.Warning           // sakit
    PresenceStatus.PERMISSION    -> Icons.Default.Description       // izin/surat
    PresenceStatus.ABSENT        -> Icons.Default.Close             // tidak hadir
    PresenceStatus.OFF_DAY       -> Icons.Default.Block             // libur
    PresenceStatus.UNKNOWN       -> Icons.Default.Info              // tidak diketahui
}

/**
 * Row jadwal mingguan — 7 kotak kecil (Sen-Min) dengan ikon kantor/rumah/libur.
 * Hari ini di-highlight dengan border biru.
 */
@Composable
fun WeeklyScheduleRow(schedule: WorkSchedule?) {
    val days = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
    val dayNums = listOf(2, 3, 4, 5, 6, 7, 1)

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { idx, day ->
            val dayNum = dayNums[idx]
            val isWorkDay = schedule?.workDays?.contains(dayNum) == true
            val isWfhDay  = schedule?.wfhDays?.contains(dayNum) == true
            val isToday   = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == dayNum

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        !isWorkDay -> CardBorder.copy(alpha = 0.3f)
                        isWfhDay   -> PurpleSoft
                        else       -> GreenSoft
                    },
                    border = if (isToday) BorderStroke(2.dp, Blue) else null
                ) {
                    Box(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                        if (!isWorkDay) {
                            Icon(Icons.Default.Remove, null, Modifier.size(14.dp), tint = TextMuted)
                        } else if (isWfhDay) {
                            Icon(Icons.Default.Home, null, Modifier.size(14.dp), tint = Purple)
                        } else {
                            Icon(Icons.Default.LocationCity, null, Modifier.size(14.dp), tint = Green)
                        }
                    }
                }
                Text(day, style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) Blue else TextMuted)
            }
        }
    }
}

/**
 * Chip info kecil (label + value) untuk detail check-in/check-out/kantor.
 */
@Composable
fun InfoChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = CardBorder.copy(alpha = 0.3f)) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.labelMedium, color = TextPrimary,
                fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Summary chip untuk team presence view — menampilkan count + label dengan filter toggle.
 */
@Composable
fun PresenceSummaryChip(
    label: String,
    count: Int,
    textColor: Color,
    bgColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) textColor else bgColor,
        border = if (selected) null else BorderStroke(0.5.dp, textColor.copy(alpha = 0.3f)),
        onClick = onClick
    ) {
        Text(
            "$count $label",
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else textColor
        )
    }
}
