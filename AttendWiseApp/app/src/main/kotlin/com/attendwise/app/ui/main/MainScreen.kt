package com.attendwise.app.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.attendwise.app.data.AttendWiseRepository
import com.attendwise.app.model.DayOfWeek
import com.attendwise.app.ui.components.AttendWiseTopBar
import com.attendwise.app.ui.screens.*

enum class BottomTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TIMETABLE("Timetable", Icons.Default.CalendarToday),
    ANALYTICS("Analytics", Icons.Default.Analytics),
    SUBJECTS("Subjects", Icons.Default.MenuBook),
    LOGS("Logs", Icons.Default.History)
}

enum class SubScreen {
    NONE,
    SINGLE_DAY
}

@Composable
fun MainScreen(
    repository: AttendWiseRepository = AttendWiseRepository.instance
) {
    val isLoggedIn by repository.isLoggedIn.collectAsState()
    val isDarkMode by repository.isDarkMode.collectAsState()
    val userProfile by repository.userProfile.collectAsState()

    var currentTab by remember { mutableStateOf(BottomTab.TIMETABLE) }
    var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
    var selectedDayForDetail by remember { mutableStateOf(DayOfWeek.WED) }

    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { email, roll -> repository.login(email, roll) },
            isDarkMode = isDarkMode,
            onToggleDarkMode = { repository.toggleDarkMode() }
        )
    } else {
        when (currentSubScreen) {
            SubScreen.SINGLE_DAY -> {
                SingleDayScheduleScreen(
                    repository = repository,
                    day = selectedDayForDetail,
                    onBack = { currentSubScreen = SubScreen.NONE }
                )
            }
            SubScreen.NONE -> {
                Scaffold(
                    topBar = {
                        AttendWiseTopBar(
                            title = currentTab.title,
                            subtitle = userProfile.semester,
                            userName = userProfile.name,
                            userEmail = userProfile.email,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { repository.toggleDarkMode() },
                            onSemesterChange = { repository.updateSemester(it) },
                            onClearAllData = { repository.clearAllData() },
                            onLogoutClick = { repository.logout() }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 6.dp
                        ) {
                            BottomTab.values().forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            BottomTab.TIMETABLE -> TimetableScreen(
                                repository = repository,
                                onViewSingleDay = { day ->
                                    selectedDayForDetail = day
                                    currentSubScreen = SubScreen.SINGLE_DAY
                                }
                            )
                            BottomTab.ANALYTICS -> AnalyticsScreen(repository = repository)
                            BottomTab.SUBJECTS -> SubjectsScreen(repository = repository)
                            BottomTab.LOGS -> HistoryLogsScreen(repository = repository)
                        }
                    }
                }
            }
        }
    }
}
