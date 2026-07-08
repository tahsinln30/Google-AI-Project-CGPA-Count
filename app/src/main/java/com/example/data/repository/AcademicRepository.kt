package com.example.data.repository

import com.example.data.dao.AcademicDao
import com.example.data.entity.Course
import com.example.data.entity.GradeRange
import com.example.data.entity.GradingSystem
import com.example.data.entity.Semester
import kotlinx.coroutines.flow.Flow

class AcademicRepository(private val dao: AcademicDao) {
    val allGradingSystems: Flow<List<GradingSystem>> = dao.getAllGradingSystems()
    val defaultGradingSystem: Flow<GradingSystem?> = dao.getDefaultGradingSystemFlow()
    val allSemesters: Flow<List<Semester>> = dao.getAllSemestersFlow()
    val allCourses: Flow<List<Course>> = dao.getAllCoursesFlow()

    suspend fun getGradingSystemById(id: Long): GradingSystem? = dao.getGradingSystemById(id)
    suspend fun getGradeRangesForSystem(systemId: Long): List<GradeRange> = dao.getGradeRangesForSystem(systemId)
    fun getGradeRangesForSystemFlow(systemId: Long): Flow<List<GradeRange>> = dao.getGradeRangesForSystemFlow(systemId)

    suspend fun insertGradingSystem(system: GradingSystem, ranges: List<GradeRange>) {
        val systemId = dao.insertGradingSystem(system)
        val updatedRanges = ranges.map { it.copy(systemId = systemId) }
        dao.insertGradeRanges(updatedRanges)
    }

    suspend fun updateGradingSystem(system: GradingSystem, ranges: List<GradeRange>) {
        dao.updateGradingSystem(system)
        dao.deleteGradeRangesBySystemId(system.id)
        val updatedRanges = ranges.map { it.copy(systemId = system.id) }
        dao.insertGradeRanges(updatedRanges)
    }

    suspend fun deleteGradingSystem(system: GradingSystem) {
        dao.deleteGradingSystem(system)
        dao.deleteGradeRangesBySystemId(system.id)
    }

    suspend fun setDefaultGradingSystem(systemId: Long) {
        dao.setDefaultGradingSystem(systemId)
    }

    suspend fun insertSemester(semester: Semester): Long = dao.insertSemester(semester)
    suspend fun deleteSemester(semester: Semester) = dao.deleteSemester(semester)

    fun getCoursesForSemester(semesterId: Long): Flow<List<Course>> = dao.getCoursesForSemesterFlow(semesterId)
    suspend fun insertCourse(course: Course) = dao.insertCourse(course)
    suspend fun updateCourse(course: Course) = dao.updateCourse(course)
    suspend fun deleteCourse(course: Course) = dao.deleteCourse(course)
}
