package com.example.data.dao

import androidx.room.*
import com.example.data.entity.Course
import com.example.data.entity.GradeRange
import com.example.data.entity.GradingSystem
import com.example.data.entity.Semester
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicDao {
    // Grading Systems
    @Query("SELECT * FROM grading_systems")
    fun getAllGradingSystems(): Flow<List<GradingSystem>>

    @Query("SELECT * FROM grading_systems WHERE id = :id")
    suspend fun getGradingSystemById(id: Long): GradingSystem?

    @Query("SELECT * FROM grading_systems WHERE isDefault = 1 LIMIT 1")
    fun getDefaultGradingSystemFlow(): Flow<GradingSystem?>

    @Query("SELECT * FROM grading_systems WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultGradingSystem(): GradingSystem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradingSystem(system: GradingSystem): Long

    @Update
    suspend fun updateGradingSystem(system: GradingSystem)

    @Transaction
    suspend fun setDefaultGradingSystem(systemId: Long) {
        clearDefaultGradingSystems()
        setGradingSystemAsDefault(systemId)
    }

    @Query("UPDATE grading_systems SET isDefault = 0")
    suspend fun clearDefaultGradingSystems()

    @Query("UPDATE grading_systems SET isDefault = 1 WHERE id = :systemId")
    suspend fun setGradingSystemAsDefault(systemId: Long)

    @Delete
    suspend fun deleteGradingSystem(system: GradingSystem)

    // Grade Ranges
    @Query("SELECT * FROM grade_ranges WHERE systemId = :systemId ORDER BY minScore DESC")
    fun getGradeRangesForSystemFlow(systemId: Long): Flow<List<GradeRange>>

    @Query("SELECT * FROM grade_ranges WHERE systemId = :systemId ORDER BY minScore DESC")
    suspend fun getGradeRangesForSystem(systemId: Long): List<GradeRange>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeRanges(ranges: List<GradeRange>)

    @Query("DELETE FROM grade_ranges WHERE systemId = :systemId")
    suspend fun deleteGradeRangesBySystemId(systemId: Long)

    // Semesters
    @Query("SELECT * FROM semesters ORDER BY createdTime DESC")
    fun getAllSemestersFlow(): Flow<List<Semester>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: Semester): Long

    @Delete
    suspend fun deleteSemester(semester: Semester)

    // Courses
    @Query("SELECT * FROM courses WHERE semesterId = :semesterId")
    fun getCoursesForSemesterFlow(semesterId: Long): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId")
    suspend fun getCoursesForSemester(semesterId: Long): List<Course>

    @Query("SELECT * FROM courses")
    fun getAllCoursesFlow(): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)
}
