package com.attendwise.app.data

import com.attendwise.app.data.firebase.FirebaseAuthManager
import com.attendwise.app.data.firebase.FirestoreManager
import com.attendwise.app.model.AttendanceRecord
import com.attendwise.app.model.AttendanceStatus
import com.attendwise.app.model.DayOfWeek
import com.attendwise.app.model.LectureSession
import com.attendwise.app.model.Subject
import com.attendwise.app.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AttendWiseRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isLoggedIn = MutableStateFlow(FirebaseAuthManager.instance.currentUser.value != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _lectures = MutableStateFlow<List<LectureSession>>(emptyList())
    val lectures: StateFlow<List<LectureSession>> = _lectures.asStateFlow()

    private val _historyRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val historyRecords: StateFlow<List<AttendanceRecord>> = _historyRecords.asStateFlow()

    private val _academicWeek = MutableStateFlow(1)
    val academicWeek: StateFlow<Int> = _academicWeek.asStateFlow()

    init {
        // Observe Auth State
        scope.launch {
            try {
                FirebaseAuthManager.instance.currentUser.collect { user ->
                    if (user != null) {
                        _isLoggedIn.value = true
                        _userProfile.update {
                            it.copy(
                                name = user.displayName ?: it.name,
                                email = user.email ?: it.email,
                                rollNumber = user.email?.substringBefore("@")?.uppercase() ?: it.rollNumber,
                                avatarUrl = user.photoUrl?.toString() ?: it.avatarUrl
                            )
                        }
                        FirestoreManager.instance.attachUserListeners(user.uid)
                    }
                }
            } catch (e: Exception) {
                // Keep default state in offline/uninitialized mode
            }
        }

        // Sync subjects from Firestore
        scope.launch {
            try {
                FirestoreManager.instance.subjects.collect { remoteSubs ->
                    _subjects.value = remoteSubs
                }
            } catch (e: Exception) {}
        }

        // Sync lectures from Firestore
        scope.launch {
            try {
                FirestoreManager.instance.lectures.collect { remoteLecs ->
                    _lectures.value = remoteLecs
                }
            } catch (e: Exception) {}
        }

        // Sync history records from Firestore
        scope.launch {
            try {
                FirestoreManager.instance.historyRecords.collect { remoteRecords ->
                    _historyRecords.value = remoteRecords
                }
            } catch (e: Exception) {}
        }

        // Sync semester from Firestore
        scope.launch {
            try {
                FirestoreManager.instance.semester.collect { remoteSem ->
                    if (remoteSem.isNotBlank()) {
                        _userProfile.update { it.copy(semester = remoteSem) }
                    }
                }
            } catch (e: Exception) {}
        }

        // Sync academic week from Firestore
        scope.launch {
            try {
                FirestoreManager.instance.academicWeek.collect { remoteWeek ->
                    if (remoteWeek > 0) {
                        _academicWeek.value = remoteWeek
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun updateSemester(newSemester: String) {
        _userProfile.update { it.copy(semester = newSemester) }
        val user = FirebaseAuthManager.instance.currentUser.value
        if (user != null) {
            FirestoreManager.instance.saveAcademicSettings(user.uid, newSemester, _academicWeek.value)
        }
    }

    fun updateAcademicWeek(newWeek: Int) {
        val week = newWeek.coerceIn(1, 24)
        _academicWeek.value = week
        val user = FirebaseAuthManager.instance.currentUser.value
        if (user != null) {
            FirestoreManager.instance.saveAcademicSettings(user.uid, _userProfile.value.semester, week)
        }
    }

    fun login(email: String = "student@university.edu", roll: String = "22BCE1042") {
        _userProfile.update { it.copy(email = email, rollNumber = roll) }
        _isLoggedIn.value = true
    }

    fun logout() {
        try {
            FirebaseAuthManager.instance.signOut()
            FirestoreManager.instance.detachListeners()
        } catch (e: Exception) {}
        _subjects.value = emptyList()
        _lectures.value = emptyList()
        _historyRecords.value = emptyList()
        _userProfile.value = UserProfile()
        _isLoggedIn.value = false
    }

    fun clearAllData(onComplete: () -> Unit = {}) {
        _subjects.value = emptyList()
        _lectures.value = emptyList()
        _historyRecords.value = emptyList()
        val user = FirebaseAuthManager.instance.currentUser.value
        if (user != null) {
            FirestoreManager.instance.clearAllUserData(user.uid, onComplete)
        } else {
            onComplete()
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.update { !it }
    }

    fun toggleLectureAttendance(lectureId: String) {
        var subjectCodeToUpdate: String? = null
        var oldStatus: AttendanceStatus = AttendanceStatus.UNMARKED
        var newStatus: AttendanceStatus = AttendanceStatus.UNMARKED

        _lectures.update { list ->
            list.map { lec ->
                if (lec.id == lectureId) {
                    subjectCodeToUpdate = lec.subjectCode
                    oldStatus = lec.status
                    newStatus = when (lec.status) {
                        AttendanceStatus.UNMARKED -> AttendanceStatus.PRESENT
                        AttendanceStatus.PRESENT -> AttendanceStatus.ABSENT
                        AttendanceStatus.ABSENT -> AttendanceStatus.UNMARKED
                        AttendanceStatus.CANCELLED -> AttendanceStatus.UNMARKED
                    }
                    lec.copy(status = newStatus)
                } else {
                    lec
                }
            }
        }

        // Update corresponding subject count if needed
        subjectCodeToUpdate?.let { code ->
            _subjects.update { subList ->
                subList.map { sub ->
                    if (sub.code == code) {
                        var attended = sub.attendedClasses
                        var total = sub.totalClasses

                        // Reverse old status
                        when (oldStatus) {
                            AttendanceStatus.PRESENT -> {
                                attended -= 1
                                total -= 1
                            }
                            AttendanceStatus.ABSENT -> {
                                total -= 1
                            }
                            else -> {}
                        }

                        // Apply new status
                        when (newStatus) {
                            AttendanceStatus.PRESENT -> {
                                attended += 1
                                total += 1
                            }
                            AttendanceStatus.ABSENT -> {
                                total += 1
                            }
                            else -> {}
                        }

                        sub.copy(
                            attendedClasses = attended.coerceAtLeast(0),
                            totalClasses = total.coerceAtLeast(1)
                        )
                    } else {
                        sub
                    }
                }
            }

            // Append to history record if marked
            if (newStatus == AttendanceStatus.PRESENT || newStatus == AttendanceStatus.ABSENT) {
                val lec = _lectures.value.find { it.id == lectureId }
                if (lec != null) {
                    val record = AttendanceRecord(
                        id = "rec_${System.currentTimeMillis()}",
                        date = "Today, 16 Oct",
                        subjectCode = lec.subjectCode,
                        subjectName = lec.subjectName,
                        time = lec.startTime,
                        room = lec.room,
                        status = newStatus
                    )
                    _historyRecords.update { listOf(record) + it }

                    // Sync to Firestore if authenticated
                    val uid = FirebaseAuthManager.instance.currentUser.value?.uid
                    if (uid != null) {
                        FirestoreManager.instance.logAttendanceEvent(uid, record)
                    }
                }
            }

            // Sync updated subject and lecture status to Firestore if authenticated
            val uid = FirebaseAuthManager.instance.currentUser.value?.uid
            if (uid != null) {
                FirestoreManager.instance.updateLectureStatus(uid, lectureId, newStatus)
                val updatedSubject = _subjects.value.find { it.code == code }
                if (updatedSubject != null) {
                    FirestoreManager.instance.saveSubject(uid, updatedSubject)
                }
            }
        }
    }

    fun markAllDayPresent(day: DayOfWeek) {
        val unMarkedLectures = _lectures.value.filter { it.day == day && it.status == AttendanceStatus.UNMARKED }
        unMarkedLectures.forEach { lec ->
            toggleLectureAttendance(lec.id)
        }
    }

    fun addSubject(code: String, name: String, professor: String, room: String, targetPct: Float = 75f) {
        val newSub = Subject(
            id = "sub_${System.currentTimeMillis()}",
            code = code.trim().uppercase(),
            name = name.trim(),
            professor = professor.trim().ifBlank { "TBA" },
            room = room.trim().ifBlank { "TBA" },
            totalClasses = 0,
            attendedClasses = 0,
            targetPercentage = targetPct
        )
        _subjects.update { it + newSub }

        // Sync to Firestore if authenticated
        val uid = FirebaseAuthManager.instance.currentUser.value?.uid
        if (uid != null) {
            FirestoreManager.instance.saveSubject(uid, newSub)
        }
    }

    fun addLecture(
        subjectCode: String,
        subjectName: String,
        professor: String,
        room: String,
        day: DayOfWeek,
        startTime: String,
        endTime: String
    ) {
        val newLec = LectureSession(
            id = "lec_${System.currentTimeMillis()}",
            subjectCode = subjectCode.trim().uppercase(),
            subjectName = subjectName.trim(),
            professor = professor.trim().ifBlank { "TBA" },
            room = room.trim().ifBlank { "TBA" },
            day = day,
            startTime = startTime,
            endTime = endTime,
            status = AttendanceStatus.UNMARKED
        )
        _lectures.update { it + newLec }

        val uid = FirebaseAuthManager.instance.currentUser.value?.uid
        if (uid != null) {
            FirestoreManager.instance.saveLecture(uid, newLec)
        }
    }

    fun deleteSubject(subjectId: String) {
        val sub = _subjects.value.find { it.id == subjectId }
        _subjects.update { list -> list.filter { it.id != subjectId } }
        if (sub != null) {
            _lectures.update { list -> list.filter { it.subjectCode != sub.code } }
        }
        val uid = FirebaseAuthManager.instance.currentUser.value?.uid
        if (uid != null) {
            FirestoreManager.instance.deleteSubject(uid, subjectId)
        }
    }

    companion object {
        val instance = AttendWiseRepository()
    }
}
