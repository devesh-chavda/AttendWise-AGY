package com.attendwise.app.model

enum class AttendanceStatus {
    UNMARKED,
    PRESENT,
    ABSENT,
    CANCELLED
}

enum class DayOfWeek(val shortName: String, val fullName: String) {
    MON("MON", "Monday"),
    TUE("TUE", "Tuesday"),
    WED("WED", "Wednesday"),
    THU("THU", "Thursday"),
    FRI("FRI", "Friday"),
    SAT("SAT", "Saturday")
}

data class Subject(
    val id: String,
    val code: String,
    val name: String,
    val professor: String,
    val room: String,
    val totalClasses: Int,
    val attendedClasses: Int,
    val targetPercentage: Float = 75.0f,
    val colorHex: Long = 0xFF00236FL
) {
    val percentage: Float
        get() = if (totalClasses > 0) (attendedClasses.toFloat() / totalClasses * 100f) else 100f

    val isAtRisk: Boolean
        get() = percentage < targetPercentage

    // How many safe bunks remaining before dropping below target
    val safeBunks: Int
        get() {
            if (percentage < targetPercentage) return 0
            // Formula: (attended) / (total + x) >= target
            // attended >= target * (total + x)
            // x <= (attended / target) - total
            val targetRatio = targetPercentage / 100f
            val maxTotal = (attendedClasses / targetRatio).toInt()
            return (maxTotal - totalClasses).coerceAtLeast(0)
        }

    // How many consecutive classes needed to get back to target
    val classesNeeded: Int
        get() {
            if (percentage >= targetPercentage) return 0
            val targetRatio = targetPercentage / 100f
            // (attended + y) / (total + y) >= targetRatio
            // attended + y >= targetRatio * total + targetRatio * y
            // y * (1 - targetRatio) >= targetRatio * total - attended
            val needed = Math.ceil(((targetRatio * totalClasses - attendedClasses) / (1.0 - targetRatio.toDouble()))).toInt()
            return needed.coerceAtLeast(1)
        }
}

data class LectureSession(
    val id: String,
    val subjectCode: String,
    val subjectName: String,
    val professor: String,
    val room: String,
    val day: DayOfWeek,
    val startTime: String,
    val endTime: String,
    val status: AttendanceStatus = AttendanceStatus.UNMARKED,
    val isOngoing: Boolean = false
)

data class AttendanceRecord(
    val id: String,
    val date: String,
    val subjectCode: String,
    val subjectName: String,
    val time: String,
    val room: String,
    val status: AttendanceStatus
)

data class UserProfile(
    val name: String = "Student",
    val rollNumber: String = "",
    val email: String = "",
    val semester: String = "Monsoon 2026",
    val academicYear: String = "2026",
    val avatarUrl: String? = null
)
