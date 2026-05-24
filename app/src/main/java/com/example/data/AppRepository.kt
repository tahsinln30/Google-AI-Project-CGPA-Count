package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class AppRepository(private val dao: AppDao) {

    val semestersWithCourses: Flow<List<SemesterWithCourses>> = dao.getSemestersWithCoursesFlow()
    val allGradeScales: Flow<List<GradeScaleEntity>> = dao.getAllGradeScalesFlow()
    val allGradeScalesWithEntries: Flow<List<GradeScaleWithEntries>> = dao.getAllGradeScalesWithEntriesFlow()
    val activeScaleWithEntries: Flow<GradeScaleWithEntries?> = dao.getActiveScaleWithEntriesFlow()

    fun getSettingFlow(key: String, defaultValue: String): Flow<String> {
        return dao.getSettingFlow(key).map { it?.value ?: defaultValue }
    }

    suspend fun saveSetting(key: String, value: String) {
        dao.saveSetting(AppSettingEntity(key = key, value = value))
    }

    suspend fun insertSemester(name: String, weight: Double = 1.0): Long {
        return dao.insertSemester(SemesterEntity(name = name, weight = weight))
    }

    suspend fun updateSemester(semester: SemesterEntity) {
        dao.updateSemester(semester)
    }

    suspend fun deleteSemester(id: Long) {
        dao.deleteSemesterById(id)
    }

    suspend fun insertCourse(semesterId: Long, name: String, credits: Double, grade: String): Long {
        return dao.insertCourse(
            CourseEntity(
                semesterId = semesterId,
                name = name,
                credits = credits,
                grade = grade
            )
        )
    }

    suspend fun updateCourse(course: CourseEntity) {
        dao.updateCourse(course)
    }

    suspend fun deleteCourse(id: Long) {
        dao.deleteCourseById(id)
    }

    suspend fun setActiveScale(scaleId: Long) {
        dao.setActiveScale(scaleId)
    }

    suspend fun deleteGradeScale(scaleId: Long) {
        // If it was active, activate standard 4.0 scale if available
        val active = dao.getActiveScale()
        if (active?.id == scaleId) {
            val scales = dao.getAllGradeScales()
            val alternative = scales.firstOrNull { it.id != scaleId }
            if (alternative != null) {
                dao.setActiveScale(alternative.id)
            }
        }
        dao.deleteGradeScaleById(scaleId)
    }

    suspend fun saveCustomScale(name: String, maxPoints: Double, entries: List<GradeEntryEntity>): Long {
        val scale = GradeScaleEntity(
            name = name,
            maxPoints = maxPoints,
            isSystem = false,
            isActive = false
        )
        return dao.saveGradeScaleWithEntries(scale, entries)
    }

    suspend fun editCustomScale(scaleId: Long, name: String, maxPoints: Double, entries: List<GradeEntryEntity>) {
        val scale = GradeScaleEntity(
            id = scaleId,
            name = name,
            maxPoints = maxPoints,
            isSystem = false,
            isActive = false // keep it active if it was already active
        )
        dao.saveGradeScaleWithEntries(scale, entries)
    }

    suspend fun seedInitialDataIfNecessary() {
        val existingScales = dao.getAllGradeScales()
        if (existingScales.isEmpty()) {
            // Seed standard 4.0 scale with university specific mappings
            val scale4Obj = GradeScaleEntity(
                name = "University 4.0 Scale",
                maxPoints = 4.0,
                isSystem = true,
                isActive = true
            )
            val scale4Id = dao.insertGradeScale(scale4Obj)
            val scale4Entries = listOf(
                GradeEntryEntity(scale4Id, "A+", 4.00),
                GradeEntryEntity(scale4Id, "A", 3.75),
                GradeEntryEntity(scale4Id, "A-", 3.50),
                GradeEntryEntity(scale4Id, "B+", 3.25),
                GradeEntryEntity(scale4Id, "B", 3.00),
                GradeEntryEntity(scale4Id, "B-", 2.75),
                GradeEntryEntity(scale4Id, "C+", 2.50),
                GradeEntryEntity(scale4Id, "C", 2.25),
                GradeEntryEntity(scale4Id, "D", 2.00),
                GradeEntryEntity(scale4Id, "F", 0.00)
            )
            dao.insertGradeEntries(scale4Entries)

            // Seed standard 5.0 scale
            val scale5Obj = GradeScaleEntity(
                name = "Standard 5.0 Scale",
                maxPoints = 5.0,
                isSystem = true,
                isActive = false
            )
            val scale5Id = dao.insertGradeScale(scale5Obj)
            val scale5Entries = listOf(
                GradeEntryEntity(scale5Id, "A", 5.0),
                GradeEntryEntity(scale5Id, "B", 4.0),
                GradeEntryEntity(scale5Id, "C", 3.0),
                GradeEntryEntity(scale5Id, "D", 2.0),
                GradeEntryEntity(scale5Id, "E", 1.0),
                GradeEntryEntity(scale5Id, "F", 0.0)
            )
            dao.insertGradeEntries(scale5Entries)

            // Seed standard 10.0 scale
            val scale10Obj = GradeScaleEntity(
                name = "Standard 10.0 Scale",
                maxPoints = 10.0,
                isSystem = true,
                isActive = false
            )
            val scale10Id = dao.insertGradeScale(scale10Obj)
            val scale10Entries = listOf(
                GradeEntryEntity(scale10Id, "O", 10.0),
                GradeEntryEntity(scale10Id, "A+", 9.0),
                GradeEntryEntity(scale10Id, "A", 8.0),
                GradeEntryEntity(scale10Id, "B+", 7.0),
                GradeEntryEntity(scale10Id, "B", 6.0),
                GradeEntryEntity(scale10Id, "C", 5.0),
                GradeEntryEntity(scale10Id, "P", 4.0),
                GradeEntryEntity(scale10Id, "F", 0.0)
            )
            dao.insertGradeEntries(scale10Entries)

            // Seed simple default coursework for university student context
            val sem1Id = dao.insertSemester(SemesterEntity(name = "Semester 1 (Freshman Fall)", weight = 1.0))
            dao.insertCourse(CourseEntity(semesterId = sem1Id, name = "Object Oriented Programming", credits = 4.0, grade = "A"))
            dao.insertCourse(CourseEntity(semesterId = sem1Id, name = "Applied Calculus I", credits = 4.0, grade = "B+"))
            dao.insertCourse(CourseEntity(semesterId = sem1Id, name = "Introduction to Physics", credits = 3.0, grade = "A-"))
            dao.insertCourse(CourseEntity(semesterId = sem1Id, name = "Physics Laboratory", credits = 1.0, grade = "A+"))

            val sem2Id = dao.insertSemester(SemesterEntity(name = "Semester 2 (Freshman Spring)", weight = 1.1)) // customizable weights
            dao.insertCourse(CourseEntity(semesterId = sem2Id, name = "Systems Programming", credits = 4.0, grade = "A-"))
            dao.insertCourse(CourseEntity(semesterId = sem2Id, name = "Calculus II", credits = 3.0, grade = "B"))
            dao.insertCourse(CourseEntity(semesterId = sem2Id, name = "Discrete Structures", credits = 3.0, grade = "A"))
            dao.insertCourse(CourseEntity(semesterId = sem2Id, name = "University Writing Seminar", credits = 2.0, grade = "B+"))
        }
    }
}
