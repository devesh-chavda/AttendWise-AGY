package com.attendwise.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.attendwise.app.data.AttendWiseRepository
import com.attendwise.app.model.DayOfWeek
import com.attendwise.app.model.Subject

@Composable
fun SubjectsScreen(
    repository: AttendWiseRepository
) {
    val subjects by repository.subjects.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyRisk by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val filteredSubjects = subjects.filter { sub ->
        (sub.code.contains(searchQuery, ignoreCase = true) || sub.name.contains(searchQuery, ignoreCase = true) || sub.professor.contains(searchQuery, ignoreCase = true)) &&
                (!filterOnlyRisk || sub.isAtRisk)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subject")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            // Search & Filter Row
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search course code, subject, professor...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !filterOnlyRisk,
                                onClick = { filterOnlyRisk = false },
                                label = { Text("All (${subjects.size})") },
                                shape = RoundedCornerShape(12.dp)
                            )
                            FilterChip(
                                selected = filterOnlyRisk,
                                onClick = { filterOnlyRisk = true },
                                label = { Text("⚠️ At Risk (<75%)") },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            )
                        }
                        if (subjects.isNotEmpty()) {
                            TextButton(
                                onClick = { showClearAllDialog = true },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear All", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Summary banner
            item {
                val riskCount = subjects.count { it.isAtRisk }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (riskCount > 0) MaterialTheme.colorScheme.errorContainer
                    else MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (riskCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (riskCount > 0) MaterialTheme.colorScheme.onErrorContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Column {
                            Text(
                                text = if (riskCount > 0) "$riskCount course(s) require immediate attendance!"
                                else "All registered courses meet the 75% requirement!",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (riskCount > 0) MaterialTheme.colorScheme.onErrorContainer
                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                            Text(
                                text = "University Debarment Rule: Minimum 75% per course required for semester exams.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = if (riskCount > 0) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }
                }
            }

    // Subject Cards List
    if (filteredSubjects.isEmpty()) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No Courses Added Yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Add your semester courses to calculate your 75% attendance threshold, room locator, and safe bunks.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Your First Course")
                    }
                }
            }
        }
    } else {
        items(filteredSubjects, key = { it.id }) { subject ->
            SubjectCard(
                subject = subject,
                onDelete = { repository.deleteSubject(subject.id) }
            )
        }
    }
}
}

// Add Subject Dialog
if (showAddDialog) {
    AddSubjectDialog(
        onDismiss = { showAddDialog = false },
        onAddSubject = { code, name, room, day, startTime, endTime ->
            repository.addSubject(code, name, "", room, 75f)
            if (day != null && startTime.isNotBlank()) {
                repository.addLecture(
                    subjectCode = code,
                    subjectName = name,
                    professor = "",
                    room = room,
                    day = day,
                    startTime = startTime,
                    endTime = endTime.ifBlank { "09:30 AM" }
                )
            }
            showAddDialog = false
        }
    )

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear All Courses?") },
            text = { Text("Are you sure you want to delete all registered courses and their timetable schedules? This will give you a fresh blank slate.") },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllDialog = false
                        repository.clearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllDialog = false }) { Text("Cancel") }
            }
        )
    }
}
}

@Composable
fun SubjectCard(
    subject: Subject,
    onDelete: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Code Badge + Status Pill + Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = subject.code,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (subject.isAtRisk) MaterialTheme.colorScheme.errorContainer
                        else if (subject.percentage >= 85f) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = if (subject.totalClasses == 0) "New Course"
                            else if (subject.isAtRisk) "Critical (<75%)"
                            else if (subject.percentage >= 85f) "Safe Standing"
                            else "Caution (75-85%)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (subject.isAtRisk) MaterialTheme.colorScheme.onErrorContainer
                                else if (subject.percentage >= 85f) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Subject",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Subject Name & Professor & Room
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (subject.professor.isNotBlank() && subject.professor != "TBA") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(subject.professor, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                    if (subject.room.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(subject.room, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                        }
                    }
                }
            }

            // Progress Bar & Percentage
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Attendance: ${subject.attendedClasses}/${subject.totalClasses} classes",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = String.format("%.1f%%", subject.percentage),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (subject.isAtRisk) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    )
                }

                LinearProgressIndicator(
                    progress = { (subject.percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (subject.isAtRisk) MaterialTheme.colorScheme.error
                    else if (subject.percentage >= 85f) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.tertiary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }

            // Margin & Bunk Advice Footer
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (subject.isAtRisk) {
                        Text(
                            text = "🚨 Must attend next ${subject.classesNeeded} classes consecutively",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else {
                        Text(
                            text = "✓ You can miss ${subject.safeBunks} more classes safely",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Text(
                        text = "Target: ${subject.targetPercentage.toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onAddSubject: (
        code: String,
        name: String,
        room: String,
        day: DayOfWeek?,
        startTime: String,
        endTime: String
    ) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var room by remember { mutableStateOf("") }
    var selectedDay by remember { mutableStateOf<DayOfWeek?>(DayOfWeek.MON) }
    var startTime by remember { mutableStateOf("08:00 AM") }
    var endTime by remember { mutableStateOf("09:30 AM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Add Course & Schedule", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code (e.g. CS309)") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Title (e.g. Cloud Computing)") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Lecture Hall / Room") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Weekly Lecture Day",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(listOf(DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.THU, DayOfWeek.FRI, DayOfWeek.SAT)) { day ->
                        val isSelected = selectedDay == day
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDay = if (isSelected) null else day },
                            label = { Text(day.fullName, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Text(
                    text = "Lecture Timing (8 AM – 7 PM)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start Time") },
                        placeholder = { Text("08:00 AM") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End Time") },
                        placeholder = { Text("09:30 AM") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        onAddSubject(
                            code.trim().uppercase(),
                            name.trim(),
                            room.ifBlank { "TBA" },
                            selectedDay,
                            startTime.ifBlank { "08:00 AM" },
                            endTime.ifBlank { "09:30 AM" }
                        )
                    }
                },
                enabled = code.isNotBlank() && name.isNotBlank()
            ) {
                Text("Add Course")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
