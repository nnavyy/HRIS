package com.ptniger.hris.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Komponen Skeleton untuk Loading Dashboard.
 * Memberikan feedback visual modern saat data metrik sedang dimuat.
 */
@Composable
fun DashboardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Skeleton Profile / Welcome Header
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerLoading()
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(16.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerLoading()
        )
        Spacer(Modifier.height(20.dp))

        // Skeleton Metric Cards Grid (2 baris x 2 kolom)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCardSkeleton(Modifier.weight(1f))
            MetricCardSkeleton(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCardSkeleton(Modifier.weight(1f))
            MetricCardSkeleton(Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))

        // Skeleton Quick Action Buttons
        Box(
            modifier = Modifier
                .fillMaxWidth(0.35f)
                .height(18.dp)
                .clip(RoundedCornerShape(6.dp))
                .shimmerLoading()
        )
        Spacer(Modifier.height(10.dp))
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmerLoading()
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun MetricCardSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(20.dp))
            .shimmerLoading()
    )
}

/**
 * Skeleton untuk daftar item (karyawan, slip gaji, atau pengajuan).
 */
@Composable
fun ListCardSkeleton(count: Int = 4, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmerLoading()
            )
        }
    }
}
