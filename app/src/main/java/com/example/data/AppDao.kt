package com.example.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SemesterWithCourses(
    @Embedded val semester: SemesterEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "semesterId"
    )
    val courses: List<CourseEntity>
)

data class GradeScaleWithEntries(
    @Embedded val scale: GradeScaleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "scaleId"
    )
    val entries: List<GradeEntryEntity>
)

@Dao
interface AppDao {
    // Semester operations
    @Query("SELECT * FROM semesters ORDER BY id ASC")
    fun getAllSemestersFlow(): Flow<List<SemesterEntity>>

    @Transaction
    @Query("SELECT * FROM semesters ORDER BY id ASC")
    fun getSemestersWithCoursesFlow(): Flow<List<SemesterWithCourses>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSemester(semester: SemesterEntity): Long

    @Update
    suspend fun updateSemester(semester: SemesterEntity)

    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun deleteSemesterById(id: Long)

    // Course operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourseById(id: Long)

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId")
    suspend fun getCoursesForSemester(semesterId: Long): List<CourseEntity>

    // Grade Scale operations
    @Query("SELECT * FROM grade_scales ORDER BY id ASC")
    suspend fun getAllGradeScales(): List<GradeScaleEntity>

    @Query("SELECT * FROM grade_scales ORDER BY id ASC")
    fun getAllGradeScalesFlow(): Flow<List<GradeScaleEntity>>

    @Transaction
    @Query("SELECT * FROM grade_scales ORDER BY id ASC")
    fun getAllGradeScalesWithEntriesFlow(): Flow<List<GradeScaleWithEntries>>

    @Transaction
    @Query("SELECT * FROM grade_scales WHERE isActive = 1 LIMIT 1")
    fun getActiveScaleWithEntriesFlow(): Flow<GradeScaleWithEntries?>

    @Query("SELECT * FROM grade_scales WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveScale(): GradeScaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeScale(scale: GradeScaleEntity): Long

    @Update
    suspend fun updateGradeScale(scale: GradeScaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeEntries(entries: List<GradeEntryEntity>)

    @Query("DELETE FROM grade_entries WHERE scaleId = :scaleId")
    suspend fun deleteGradeEntriesForScale(scaleId: Long)

    @Transaction
    suspend fun saveGradeScaleWithEntries(scale: GradeScaleEntity, entries: List<GradeEntryEntity>): Long {
        val scaleId = insertGradeScale(scale)
        val updatedEntries = entries.map { it.copy(scaleId = scaleId) }
        deleteGradeEntriesForScale(scaleId)
        insertGradeEntries(updatedEntries)
        return scaleId
    }

    @Transaction
    suspend fun setActiveScale(scaleId: Long) {
        deactivateAllScales()
        activateScale(scaleId)
    }

    @Query("UPDATE grade_scales SET isActive = 0")
    suspend fun deactivateAllScales()

    @Query("UPDATE grade_scales SET isActive = 1 WHERE id = :scaleId")
    suspend fun activateScale(scaleId: Long)

    @Query("DELETE FROM grade_scales WHERE id = :id")
    suspend fun deleteGradeScaleById(id: Long)

    // Settings operations
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSettingEntity?

    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSettingFlow(key: String): Flow<AppSettingEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSettingEntity)
}
