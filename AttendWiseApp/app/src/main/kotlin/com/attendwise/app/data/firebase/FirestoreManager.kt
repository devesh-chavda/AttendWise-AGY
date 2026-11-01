package com.attendwise.app.data.firebase

import android.util.Log
import com.attendwise.app.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FirestoreManager {

    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }

    private var subjectsListener: ListenerRegistration? = null
    private var lecturesListener: ListenerRegistration? = null
    private var historyListener: ListenerRegistration? = null
    private var userDocListener: ListenerRegistration? = null

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _lectures = MutableStateFlow<List<LectureSession>>(emptyList())
    val lectures: StateFlow<List<LectureSession>> = _lectures.asStateFlow()

    private val _historyRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val historyRecords: StateFlow<List<AttendanceRecord>> = _historyRecords.asStateFlow()

    private val _semester = MutableStateFlow("Monsoon 2026")
    val semester: StateFlow<String> = _semester.asStateFlow()

    private val _academicWeek = MutableStateFlow(1)
    val academicWeek: StateFlow<Int> = _academicWeek.asStateFlow()

    private val mockSubjectCodes = setOf("CS301", "CS302", "CS303", "CS304", "CSE305", "CS305")

    fun attachUserListeners(userId: String) {
        detachListeners()

        val userDoc = firestore.collection("users").document(userId)

        // 0. Listen to Academic Settings
        userDocListener = userDoc.addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null && snapshot.exists()) {
                val sem = snapshot.getString("semester")
                if (!sem.isNullOrBlank()) {
                    _semester.value = sem
                }
                val week = snapshot.getLong("academicWeek")?.toInt()
                if (week != null && week > 0) {
                    _academicWeek.value = week
                }
            }
        }

        // 1. Listen to Subjects
        subjectsListener = userDoc.collection("subjects").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("FirestoreManager", "Subjects listen error", error)
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                // Purge legacy mock documents from cloud firestore
                val legacyDocs = snapshot.documents.filter { doc ->
                    doc.id.startsWith("sub_") || doc.getString("code") in mockSubjectCodes
                }
                for (legacy in legacyDocs) {
                    legacy.reference.delete()
                }

                // Filter out legacy items from state
                val validDocs = snapshot.documents.filterNot { doc ->
                    doc.id.startsWith("sub_") || doc.getString("code") in mockSubjectCodes
                }

                val list = validDocs.mapNotNull { doc ->
                    try {
                        Subject(
                            id = doc.id,
                            code = doc.getString("code") ?: "",
                            name = doc.getString("name") ?: "",
                            professor = doc.getString("professor") ?: "",
                            room = doc.getString("room") ?: "",
                            totalClasses = (doc.getLong("totalClasses") ?: 0L).toInt(),
                            attendedClasses = (doc.getLong("attendedClasses") ?: 0L).toInt(),
                            targetPercentage = (doc.getDouble("targetPercentage") ?: 75.0).toFloat(),
                            colorHex = doc.getLong("colorHex") ?: 0xFF00236FL
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                _subjects.value = list
            } else {
                _subjects.value = emptyList()
            }
        }

        // 2. Listen to Lectures
        lecturesListener = userDoc.collection("lectures").addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w("FirestoreManager", "Lectures listen error", error)
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                // Purge legacy mock documents from cloud firestore
                val legacyDocs = snapshot.documents.filter { doc ->
                    doc.id.startsWith("lec_") || doc.getString("subjectCode") in mockSubjectCodes
                }
                for (legacy in legacyDocs) {
                    legacy.reference.delete()
                }

                // Filter out legacy items from state
                val validDocs = snapshot.documents.filterNot { doc ->
                    doc.id.startsWith("lec_") || doc.getString("subjectCode") in mockSubjectCodes
                }

                val list = validDocs.mapNotNull { doc ->
                    try {
                        val dayStr = doc.getString("day") ?: "WED"
                        val day = DayOfWeek.values().find { it.name == dayStr } ?: DayOfWeek.WED
                        val statusStr = doc.getString("status") ?: "UNMARKED"
                        val status = AttendanceStatus.values().find { it.name == statusStr } ?: AttendanceStatus.UNMARKED

                        LectureSession(
                            id = doc.id,
                            subjectCode = doc.getString("subjectCode") ?: "",
                            subjectName = doc.getString("subjectName") ?: "",
                            professor = doc.getString("professor") ?: "",
                            room = doc.getString("room") ?: "",
                            day = day,
                            startTime = doc.getString("startTime") ?: "",
                            endTime = doc.getString("endTime") ?: "",
                            status = status,
                            isOngoing = doc.getBoolean("isOngoing") ?: false
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                _lectures.value = list
            } else {
                _lectures.value = emptyList()
            }
        }

        // 3. Listen to History
        historyListener = userDoc.collection("history")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreManager", "History listen error", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Purge legacy mock documents from cloud firestore
                    val legacyDocs = snapshot.documents.filter { doc ->
                        doc.id.startsWith("rec_") || doc.getString("subjectCode") in mockSubjectCodes
                    }
                    for (legacy in legacyDocs) {
                        legacy.reference.delete()
                    }

                    // Filter out legacy items from state
                    val validDocs = snapshot.documents.filterNot { doc ->
                        doc.id.startsWith("rec_") || doc.getString("subjectCode") in mockSubjectCodes
                    }

                    val list = validDocs.mapNotNull { doc ->
                        try {
                            val statusStr = doc.getString("status") ?: "PRESENT"
                            val status = AttendanceStatus.values().find { it.name == statusStr } ?: AttendanceStatus.PRESENT
                            AttendanceRecord(
                                id = doc.id,
                                date = doc.getString("date") ?: "",
                                subjectCode = doc.getString("subjectCode") ?: "",
                                subjectName = doc.getString("subjectName") ?: "",
                                time = doc.getString("time") ?: "",
                                room = doc.getString("room") ?: "",
                                status = status
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _historyRecords.value = list
                }
            }
    }

    fun detachListeners() {
        subjectsListener?.remove()
        subjectsListener = null
        lecturesListener?.remove()
        lecturesListener = null
        historyListener?.remove()
        historyListener = null
        userDocListener?.remove()
        userDocListener = null
    }

    fun saveAcademicSettings(userId: String, semester: String, academicWeek: Int) {
        _semester.value = semester
        _academicWeek.value = academicWeek
        val data = hashMapOf(
            "semester" to semester,
            "academicWeek" to academicWeek
        )
        firestore.collection("users").document(userId).set(data, SetOptions.merge())
    }

    fun saveSubject(userId: String, subject: Subject) {
        val data = hashMapOf(
            "code" to subject.code,
            "name" to subject.name,
            "professor" to subject.professor,
            "room" to subject.room,
            "totalClasses" to subject.totalClasses,
            "attendedClasses" to subject.attendedClasses,
            "targetPercentage" to subject.targetPercentage,
            "colorHex" to subject.colorHex
        )
        firestore.collection("users").document(userId)
            .collection("subjects").document(subject.id)
            .set(data, SetOptions.merge())
    }

    fun updateLectureStatus(userId: String, lectureId: String, newStatus: AttendanceStatus) {
        firestore.collection("users").document(userId)
            .collection("lectures").document(lectureId)
            .set(mapOf("status" to newStatus.name), SetOptions.merge())
    }

    fun logAttendanceEvent(userId: String, record: AttendanceRecord) {
        val data = hashMapOf(
            "date" to record.date,
            "subjectCode" to record.subjectCode,
            "subjectName" to record.subjectName,
            "time" to record.time,
            "room" to record.room,
            "status" to record.status.name,
            "timestamp" to System.currentTimeMillis()
        )
        firestore.collection("users").document(userId)
            .collection("history").document(record.id)
            .set(data)
    }

    fun saveLecture(userId: String, lecture: LectureSession) {
        val data = hashMapOf(
            "subjectCode" to lecture.subjectCode,
            "subjectName" to lecture.subjectName,
            "professor" to lecture.professor,
            "room" to lecture.room,
            "day" to lecture.day.name,
            "startTime" to lecture.startTime,
            "endTime" to lecture.endTime,
            "status" to lecture.status.name,
            "isOngoing" to lecture.isOngoing
        )
        firestore.collection("users").document(userId)
            .collection("lectures").document(lecture.id)
            .set(data, SetOptions.merge())
    }

    fun deleteSubject(userId: String, subjectId: String) {
        firestore.collection("users").document(userId)
            .collection("subjects").document(subjectId)
            .delete()
    }

    fun deleteLecture(userId: String, lectureId: String) {
        firestore.collection("users").document(userId)
            .collection("lectures").document(lectureId)
            .delete()
    }

    fun clearAllUserData(userId: String, onComplete: () -> Unit = {}) {
        val userDoc = firestore.collection("users").document(userId)

        userDoc.collection("subjects").get().addOnSuccessListener { subSnap ->
            for (doc in subSnap.documents) {
                doc.reference.delete()
            }
            userDoc.collection("lectures").get().addOnSuccessListener { lecSnap ->
                for (doc in lecSnap.documents) {
                    doc.reference.delete()
                }
                userDoc.collection("history").get().addOnSuccessListener { histSnap ->
                    for (doc in histSnap.documents) {
                        doc.reference.delete()
                    }
                    _subjects.value = emptyList()
                    _lectures.value = emptyList()
                    _historyRecords.value = emptyList()
                    onComplete()
                }.addOnFailureListener { onComplete() }
            }.addOnFailureListener { onComplete() }
        }.addOnFailureListener { onComplete() }
    }

    companion object {
        val instance = FirestoreManager()
    }
}
