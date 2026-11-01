package com.attendwise.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendwise.app.data.AttendWiseRepository
import com.attendwise.app.model.AttendanceStatus
import com.attendwise.app.model.DayOfWeek
import com.attendwise.app.model.LectureSession
import com.attendwise.app.ui.components.CircularProgressMeter
import com.attendwise.app.ui.components.StatusBadge

@Composable
fun TimetableScreen(
    repository: AttendWiseRepository,
    onViewSingleDay: (DayOfWeek) -> Unit = {}
) {
    val subjects by repository.subjects.collectAsState()
    val lectures by repository.lectures.collectAsState()
    val academicWeek by repository.academicWeek.collectAsState()

    val todayDow = remember {
        val cal = java.util.Calendar.getInstance()
        when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
            java.util.Calendar.MONDAY -> DayOfWeek.MON
            java.util.Calendar.TUESDAY -> DayOfWeek.TUE
            java.util.Calendar.WEDNESDAY -> DayOfWeek.WED
            java.util.Calendar.THURSDAY -> DayOfWeek.THU
            java.util.Calendar.FRIDAY -> DayOfWeek.FRI
            java.util.Calendar.SATURDAY -> DayOfWeek.SAT
            else -> DayOfWeek.MON
        }
    }
    var selectedDay by remember { mutableStateOf(todayDow) }
    var selectedLectureForRadar by remember { mutableStateOf<LectureSession?>(null) }
    var showWeekPicker by remember { mutableStateOf(false) }

    // Calculate Cumulative Standing
    val totalAttended = subjects.sumOf { it.attendedClasses }
    val totalClasses = subjects.sumOf { it.totalClasses }
    val cumulativePercentage = if (totalClasses > 0) (totalAttended.toFloat() / totalClasses * 100f) else 0f

    val dayLectures = lectures.filter { it.day == selectedDay }
    val markedCount = dayLectures.count { it.status != AttendanceStatus.UNMARKED }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // 1. Cumulative Standing Metric Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CUMULATIVE STANDING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = String.format("%.1f%%", cumulativePercentage),
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (cumulativePercentage >= 85f) MaterialTheme.colorScheme.secondaryContainer
                                    else if (cumulativePercentage >= 75f) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = if (cumulativePercentage >= 85f) "Safe: ≥85%"
                                        else if (cumulativePercentage >= 75f) "Warning: 75–85%"
                                        else "Danger: <75%",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (cumulativePercentage >= 85f) MaterialTheme.colorScheme.onSecondaryContainer
                                            else if (cumulativePercentage >= 75f) MaterialTheme.colorScheme.onTertiaryContainer
                                            else MaterialTheme.colorScheme.onErrorContainer
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        CircularProgressMeter(
                            percentage = cumulativePercentage,
                            size = 56.dp,
                            strokeWidth = 5.dp,
                            color = if (cumulativePercentage >= 85f) MaterialTheme.colorScheme.secondary
                            else if (cumulativePercentage >= 75f) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error
                        )
                    }

                    // Campus Thresholds Legend
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Campus Thresholds",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Min. Required 75%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LegendItem(color = MaterialTheme.colorScheme.secondary, label = "≥90% Safe")
                                LegendItem(color = MaterialTheme.colorScheme.tertiary, label = "80-89% Caution")
                                LegendItem(color = MaterialTheme.colorScheme.error, label = "<80% Critical")
                            }
                        }
                    }
                }
            }
        }

        // 2. Weekday Tab Selector & Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarViewWeek,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        IconButton(
                            onClick = { repository.updateAcademicWeek(academicWeek - 1) },
                            enabled = academicWeek > 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Week", modifier = Modifier.size(20.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable { showWeekPicker = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Academic Week $academicWeek",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        IconButton(
                            onClick = { repository.updateAcademicWeek(academicWeek + 1) },
                            enabled = academicWeek < 24,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Week", modifier = Modifier.size(20.dp))
                        }
                    }

                    // Campus Timing Badge (08:00 AM – 07:00 PM)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "8 AM – 7 PM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Static Day Tabs: Monday to Saturday
                val staticDays = listOf(
                    DayOfWeek.MON,
                    DayOfWeek.TUE,
                    DayOfWeek.WED,
                    DayOfWeek.THU,
                    DayOfWeek.FRI,
                    DayOfWeek.SAT
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    staticDays.forEach { day ->
                        val isSelected = selectedDay == day
                        val isToday = todayDow == day
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable { selectedDay = day }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = day.shortName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp)
                                            .size(4.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                                else MaterialTheme.colorScheme.secondary
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Section Banner: Day Counter + Quick Batch Mark
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedDay.fullName} • $markedCount of ${dayLectures.size} Marked",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                if (dayLectures.any { it.status == AttendanceStatus.UNMARKED }) {
                    TextButton(
                        onClick = { repository.markAllDayPresent(selectedDay) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark All Present", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 4. Lecture Cards
        if (dayLectures.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventAvailable,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (lectures.isEmpty()) "Your Timetable is Fresh & Clean"
                            else "No lectures scheduled for ${selectedDay.fullName}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (lectures.isEmpty()) "Tap the 'Subjects' tab below to add your courses and weekly schedule."
                            else "Enjoy your free period or check upcoming days.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            items(dayLectures, key = { it.id }) { lecture ->
                val subject = subjects.find { it.code == lecture.subjectCode }
                LectureCard(
                    lecture = lecture,
                    subject = subject,
                    onToggleAttendance = { repository.toggleLectureAttendance(lecture.id) },
                    onRadarClick = { selectedLectureForRadar = lecture }
                )
            }
        }
    }

    // Room Radar Dialog
    selectedLectureForRadar?.let { lec ->
        AlertDialog(
            onDismissRequest = { selectedLectureForRadar = null },
            icon = { Icon(Icons.Default.NearMe, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Lecture Room Radar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "${lec.subjectCode}: ${lec.subjectName}", fontWeight = FontWeight.Bold)
                    Text(text = "📍 Room: ${lec.room}")
                    Text(text = "⏰ Time: ${lec.startTime} – ${lec.endTime}")
                    Text(text = "👨‍🏫 Faculty: ${lec.professor}")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🧭 Navigation Tip: Take elevator to 3rd Floor, turn right at Academic Block B.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedLectureForRadar = null }) {
                    Text("Got It")
                }
            }
        )
    }

    if (showWeekPicker) {
        AlertDialog(
            onDismissRequest = { showWeekPicker = false },
            icon = {
                Icon(Icons.Default.CalendarViewWeek, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Academic Week Selector", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Jump directly to your college semester week:",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    val weeks = (1..16).toList()
                    weeks.chunked(4).forEach { rowWeeks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowWeeks.forEach { w ->
                                val isSel = w == academicWeek
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            repository.updateAcademicWeek(w)
                                            showWeekPicker = false
                                        }
                                ) {
                                    Text(
                                        text = "W$w",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showWeekPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun LectureCard(
    lecture: LectureSession,
    subject: com.attendwise.app.model.Subject?,
    onToggleAttendance: () -> Unit,
    onRadarClick: () -> Unit
) {
    val indicatorColor = when (lecture.status) {
        AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.secondary
        AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
        AttendanceStatus.UNMARKED -> MaterialTheme.colorScheme.outlineVariant
        AttendanceStatus.CANCELLED -> MaterialTheme.colorScheme.outline
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Status Stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(indicatorColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header: Time + Room Radar button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "${lecture.startTime} – ${lecture.endTime}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (lecture.isOngoing) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    text = "ONGOING",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Hall radar button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onRadarClick() }
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Radar",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = lecture.room,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Subject Title & Professor
                Column {
                    Text(
                        text = "${lecture.subjectCode} • ${lecture.subjectName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (lecture.professor.isNotBlank() && lecture.professor != "TBA") {
                        Text(
                            text = lecture.professor,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Bottom row: Subject stats & Status toggle button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        subject?.let { sub ->
                            Text(
                                text = "Attended: ${sub.attendedClasses}/${sub.totalClasses} (${String.format("%.1f", sub.percentage)}%)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (sub.isAtRisk) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = if (sub.isAtRisk) "⚠️ Attend next ${sub.classesNeeded} classes"
                                else "✓ ${sub.safeBunks} safe bunks left",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (sub.isAtRisk) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.secondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    // Interactive mark button
                    IconButton(
                        onClick = onToggleAttendance,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                when (lecture.status) {
                                    AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.secondary
                                    AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.error
                                    AttendanceStatus.UNMARKED -> MaterialTheme.colorScheme.surfaceContainerHigh
                                    AttendanceStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceContainerHigh
                                }
                            )
                    ) {
                        Icon(
                            imageVector = when (lecture.status) {
                                AttendanceStatus.PRESENT -> Icons.Default.Check
                                AttendanceStatus.ABSENT -> Icons.Default.Close
                                AttendanceStatus.UNMARKED -> Icons.Default.CheckBoxOutlineBlank
                                AttendanceStatus.CANCELLED -> Icons.Default.Block
                            },
                            contentDescription = "Toggle Attendance",
                            tint = when (lecture.status) {
                                AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.onSecondary
                                AttendanceStatus.ABSENT -> MaterialTheme.colorScheme.onError
                                AttendanceStatus.UNMARKED -> MaterialTheme.colorScheme.onSurfaceVariant
                                AttendanceStatus.CANCELLED -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
