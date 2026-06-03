package com.ptniger.hris.ui.agreement

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ptniger.hris.ui.theme.*

// ─── Preference helper ───────────────────────────────────────────────────────

private const val PREF_NAME = "hris_agreement"
private const val KEY_AGREED = "user_agreed_v1"

fun hasAgreed(context: Context): Boolean {
    return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_AGREED, false)
}

fun saveAgreement(context: Context) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_AGREED, true)
        .apply()
}

// ─── Agreement Screen ─────────────────────────────────────────────────────────

@Composable
fun AgreementScreen(onAgreed: () -> Unit) {
    val context = LocalContext.current
    var agreedTerms by remember { mutableStateOf(false) }
    var agreedPrivacy by remember { mutableStateOf(false) }
    var showTermsDetail by remember { mutableStateOf(false) }
    var showPrivacyDetail by remember { mutableStateOf(false) }

    val allAgreed = agreedTerms && agreedPrivacy

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(40.dp))

            // Header logo area
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(BlueDark, Blue, Color(0xFF60A5FA)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "HR",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Before You Continue",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Please read and accept the following agreements to access the HRIS application. These documents govern how your personal and employment data is collected, used, and protected.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(28.dp))

            // Terms of Service Card
            AgreementCard(
                title = "Terms of Service",
                subtitle = "User Agreement & Permitted Use",
                icon = Icons.Default.Description,
                iconBg = BlueSoft,
                iconColor = Blue,
                isChecked = agreedTerms,
                isExpanded = showTermsDetail,
                onToggleExpand = { showTermsDetail = !showTermsDetail },
                onCheck = { agreedTerms = it },
                summaryPoints = listOf(
                    "The Application is for authorized PT Niger employees and personnel only.",
                    "You may not share credentials or access data beyond your assigned role.",
                    "Fraudulent attendance submissions and GPS manipulation are prohibited.",
                    "Unauthorized disclosure of colleague salary or personal data is prohibited.",
                    "Account access will be revoked upon end of employment.",
                    "Disputes are governed by Indonesian law and subject to PN Jakarta Pusat jurisdiction."
                )
            )

            Spacer(Modifier.height(14.dp))

            // Privacy Policy Card
            AgreementCard(
                title = "Privacy Policy",
                subtitle = "Data Collection, Use & Your Rights",
                icon = Icons.Default.Policy,
                iconBg = PurpleSoft,
                iconColor = Purple,
                isChecked = agreedPrivacy,
                isExpanded = showPrivacyDetail,
                onToggleExpand = { showPrivacyDetail = !showPrivacyDetail },
                onCheck = { agreedPrivacy = it },
                summaryPoints = listOf(
                    "We collect identity, attendance (including GPS location), leave, payroll, KPI, and profile data.",
                    "Location is captured only at the moment of attendance check-in or check-out, never continuously.",
                    "Your data is stored securely in Firebase (Google) infrastructure with AES-256 encryption.",
                    "Data is retained for 5–10 years as required by Indonesian labor and tax law.",
                    "You have the right to access, correct, delete, and port your personal data.",
                    "Compliant with UU PDP No. 27/2022 (Indonesia) and GDPR 2016/679 (EU)."
                )
            )

            Spacer(Modifier.height(28.dp))

            // Legal notice text
            Text(
                buildAnnotatedString {
                    append("By tapping \"I Agree and Continue\", you confirm that you have read and agree to the ")
                    withStyle(SpanStyle(color = Blue, fontWeight = FontWeight.SemiBold)) {
                        append("Terms of Service")
                    }
                    append(" and ")
                    withStyle(SpanStyle(color = Purple, fontWeight = FontWeight.SemiBold)) {
                        append("Privacy Policy")
                    }
                    append(" of the HRIS Mobile Application operated by PT Niger.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.height(16.dp))

            // Agree button
            Button(
                onClick = {
                    saveAgreement(context)
                    onAgreed()
                },
                enabled = allAgreed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Blue,
                    disabledContainerColor = CardBorder
                )
            ) {
                Text(
                    "I Agree and Continue",
                    fontWeight = FontWeight.Bold,
                    color = if (allAgreed) Color.White else TextMuted
                )
            }

            Spacer(Modifier.height(12.dp))

            // Decline note
            if (!allAgreed) {
                Text(
                    "You must accept both agreements to use the application.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Agreement Card ───────────────────────────────────────────────────────────

@Composable
private fun AgreementCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconColor: Color,
    isChecked: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCheck: (Boolean) -> Unit,
    summaryPoints: List<String>
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isChecked) 1.5.dp else 1.dp,
                color = if (isChecked) iconColor.copy(alpha = 0.4f) else CardBorder,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isChecked) iconBg.copy(alpha = 0.25f) else Surface,
        shadowElevation = if (isChecked) 0.dp else 1.dp
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }

                Spacer(Modifier.width(12.dp))

                // Title and subtitle
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }

                // Checkbox
                IconButton(onClick = { onCheck(!isChecked) }) {
                    Icon(
                        imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (isChecked) "Agreed" else "Not agreed",
                        tint = if (isChecked) iconColor else TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Expand/collapse toggle
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleExpand() }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isExpanded) "Hide summary" else "View summary",
                    style = MaterialTheme.typography.labelMedium,
                    color = iconColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isExpanded) "^" else "v",
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor
                )
            }

            // Expandable summary
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = CardBorder)
                    Spacer(Modifier.height(4.dp))
                    summaryPoints.forEach { point ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = iconColor,
                                modifier = Modifier.padding(top = 1.dp, end = 8.dp)
                            )
                            Text(
                                point,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            // Agree label below checkbox
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onCheck(!isChecked) }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isChecked) iconColor else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "I have read and agree to the $title",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isChecked) iconColor else TextSecondary,
                    fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
