package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AcademicDao
import com.example.data.entity.Course
import com.example.data.entity.GradeRange
import com.example.data.entity.GradingSystem
import com.example.data.entity.Semester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [GradingSystem::class, GradeRange::class, Semester::class, Course::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun academicDao(): AcademicDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cgpa_count_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDefaultGradingSystem(database.academicDao())
                }
            }
        }

        private suspend fun populateDefaultGradingSystem(dao: AcademicDao) {
            val defaultSystemId = dao.insertGradingSystem(
                GradingSystem(
                    name = "University Level (Default)",
                    isDefault = true,
                    isSystemBuiltIn = true
                )
            )

            val ranges = listOf(
                GradeRange(systemId = defaultSystemId, grade = "A+", minScore = 80, maxScore = 100, gradePoint = 4.00),
                GradeRange(systemId = defaultSystemId, grade = "A", minScore = 75, maxScore = 79, gradePoint = 3.75),
                GradeRange(systemId = defaultSystemId, grade = "A-", minScore = 70, maxScore = 74, gradePoint = 3.50),
                GradeRange(systemId = defaultSystemId, grade = "B+", minScore = 65, maxScore = 69, gradePoint = 3.25),
                GradeRange(systemId = defaultSystemId, grade = "B", minScore = 60, maxScore = 64, gradePoint = 3.00),
                GradeRange(systemId = defaultSystemId, grade = "B-", minScore = 55, maxScore = 59, gradePoint = 2.75),
                GradeRange(systemId = defaultSystemId, grade = "C+", minScore = 50, maxScore = 54, gradePoint = 2.50),
                GradeRange(systemId = defaultSystemId, grade = "C", minScore = 45, maxScore = 49, gradePoint = 2.25),
                GradeRange(systemId = defaultSystemId, grade = "D", minScore = 40, maxScore = 44, gradePoint = 2.00),
                GradeRange(systemId = defaultSystemId, grade = "F", minScore = 0, maxScore = 39, gradePoint = 0.00)
            )
            dao.insertGradeRanges(ranges)

            val collegeSystemId = dao.insertGradingSystem(
                GradingSystem(
                    name = "College Level (Default)",
                    isDefault = false,
                    isSystemBuiltIn = true
                )
            )
            val collegeRanges = listOf(
                GradeRange(systemId = collegeSystemId, grade = "A+", minScore = 90, maxScore = 100, gradePoint = 4.00),
                GradeRange(systemId = collegeSystemId, grade = "A", minScore = 85, maxScore = 89, gradePoint = 3.75),
                GradeRange(systemId = collegeSystemId, grade = "A-", minScore = 80, maxScore = 84, gradePoint = 3.50),
                GradeRange(systemId = collegeSystemId, grade = "B+", minScore = 75, maxScore = 79, gradePoint = 3.25),
                GradeRange(systemId = collegeSystemId, grade = "B", minScore = 70, maxScore = 74, gradePoint = 3.00),
                GradeRange(systemId = collegeSystemId, grade = "B-", minScore = 65, maxScore = 69, gradePoint = 2.75),
                GradeRange(systemId = collegeSystemId, grade = "C+", minScore = 60, maxScore = 64, gradePoint = 2.50),
                GradeRange(systemId = collegeSystemId, grade = "C", minScore = 55, maxScore = 59, gradePoint = 2.25),
                GradeRange(systemId = collegeSystemId, grade = "D", minScore = 40, maxScore = 54, gradePoint = 2.00),
                GradeRange(systemId = collegeSystemId, grade = "F", minScore = 0, maxScore = 39, gradePoint = 0.00)
            )
            dao.insertGradeRanges(collegeRanges)

            val schoolSystemId = dao.insertGradingSystem(
                GradingSystem(
                    name = "School Level (Default)",
                    isDefault = false,
                    isSystemBuiltIn = true
                )
            )
            val schoolRanges = listOf(
                GradeRange(systemId = schoolSystemId, grade = "A", minScore = 90, maxScore = 100, gradePoint = 4.00),
                GradeRange(systemId = schoolSystemId, grade = "B", minScore = 80, maxScore = 89, gradePoint = 3.00),
                GradeRange(systemId = schoolSystemId, grade = "C", minScore = 70, maxScore = 79, gradePoint = 2.00),
                GradeRange(systemId = schoolSystemId, grade = "D", minScore = 60, maxScore = 69, gradePoint = 1.00),
                GradeRange(systemId = schoolSystemId, grade = "F", minScore = 0, maxScore = 59, gradePoint = 0.00)
            )
            dao.insertGradeRanges(schoolRanges)
        }
    }
}
