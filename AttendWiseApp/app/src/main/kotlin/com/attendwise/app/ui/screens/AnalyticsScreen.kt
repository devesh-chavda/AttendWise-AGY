package com.attendwise.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendwise.app.data.AttendWiseRepository
import com.attendwise.app.ui.components.CircularProgressMeter

@Composable
fun AnalyticsScreen(
    repository: AttendWiseRepository
) {
    val subjects by repository.subjects.collectAsState()

    val totalAttended = subjects.sumOf { it.attendedClasses }
    val totalClasses = subjects.sumOf { it.totalClasses }
    val overallPercentage = if (totalClasses > 0) (totalAttended.toFloat() / totalClasses * 100f) else 0f
    val atRiskSubjects = subjects.filter { it.isAtRisk }

    // Simulator State
    var simulatedBunks by remember { mutableIntStateOf(0) }
    var selectedSimSubjectIndex by remember { mutableIntStateOf(0) }

    val simSubject = subjects.getOrNull(selectedSimSubjectIndex) ?: subjects.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // 1. Overall Standing & Health Ring
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SEMESTER ATTENDANCE INDEX",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = String.format("%.1f%%", overallPercentage),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "$totalAttended attended / $totalClasses conducted",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    CircularProgressMeter(
                        percentage = overallPercentage,
                        size = 80.dp,
                        strokeWidth = 8.dp,
                        color = if (overallPercentage >= 85f) MaterialTheme.colorScheme.secondary
                        else if (overallPercentage >= 75f) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (subjects.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No Analytics Available Yet",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Add your enrolled subjects under the Subjects tab to track your semester attendance index, debarment alerts, and bunk planning simulator.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        } else {
            // 2. Risk Alert Banner (Debarment Warning)
            item {
                if (atRiskSubjects.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "ATTENDANCE RISK ALERT (${atRiskSubjects.size} COURSE)",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                )
                            }

                            atRiskSubjects.forEach { sub ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${sub.code} • ${sub.name}",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "Current: ${String.format("%.1f", sub.percentage)}% • Needs ${sub.classesNeeded} more classes",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.errorContainer
                                        ) {
                                            Text(
                                                text = "DEBARMENT RISK",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    fontSize = 9.sp
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Interactive Bunk Planner / Margin Simulator
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "Smart Bunk Planner & Simulator",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Text(
                            text = "Simulate how skipping upcoming lectures affects your attendance percentage and debarment status.",
                            style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Subject Selector Chips for Simulation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        subjects.take(4).forEachIndexed { index, sub ->
                            FilterChip(
                                selected = selectedSimSubjectIndex == index,
                                onClick = {
                                    selectedSimSubjectIndex = index
                                    simulatedBunks = 0
                                },
                                label = { Text(sub.code, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    simSubject?.let { sub ->
                        val futureTotal = sub.totalClasses + simulatedBunks
                        val futureAttended = sub.attendedClasses
                        val projectedPct = if (futureTotal > 0) (futureAttended.toFloat() / futureTotal * 100f) else 0f
                        val projectedSafe = projectedPct >= sub.targetPercentage

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Simulate Missing Classes: $simulatedBunks",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    FilledTonalIconButton(
                                        onClick = { simulatedBunks = (simulatedBunks - 1).coerceAtLeast(0) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalIconButton(
                                        onClick = { simulatedBunks = (simulatedBunks + 1).coerceAtMost(10) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Slider(
                                value = simulatedBunks.toFloat(),
                                onValueChange = { simulatedBunks = it.toInt() },
                                valueRange = 0f..8f,
                                steps = 7
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Projected Attendance:",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${String.format("%.1f", sub.percentage)}% ➔ ${String.format("%.1f", projectedPct)}%",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (projectedSafe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                                    )
                                )
                            }

                            Text(
                                text = if (projectedSafe) "✓ Safe! You remain above the 75% threshold."
                                else "🚨 Debarment Alert: Skipping $simulatedBunks classes drops you below 75%!",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (projectedSafe) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Subject-wise Comparative Breakdown
        item {
            Text(
                text = "Course Attendance Breakdown",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(subjects) { sub ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${sub.code} • ${sub.name}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = String.format("%.1f%%", sub.percentage),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (sub.isAtRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    LinearProgressIndicator(
                        progress = { (sub.percentage / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (sub.isAtRisk) MaterialTheme.colorScheme.error
                        else if (sub.percentage >= 85f) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.tertiary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${sub.attendedClasses} attended of ${sub.totalClasses}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Text(
                            text = if (sub.isAtRisk) "🚨 -${sub.classesNeeded} classes to safe" else "✓ +${sub.safeBunks} safe bunks",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sub.isAtRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }
        }
    }
}
}
