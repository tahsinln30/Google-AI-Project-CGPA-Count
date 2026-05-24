package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val weight: Double = 1.0 // customizable weighting for each semester
)

@Entity(
    tableName = "courses",
    foreignKeys = [
        ForeignKey(
            entity = SemesterEntity::class,
            parentColumns = ["id"],
            childColumns = ["semesterId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["semesterId"])]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semesterId: Long,
    val name: String,
    val credits: Double, // customizable credit weight
    val grade: String // e.g. "A", "B+", etc.
)

@Entity(tableName = "grade_scales")
data class GradeScaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val maxPoints: Double,
    val isSystem: Boolean = false, // true for pre-populated scales
    val isActive: Boolean = false
)

@Entity(
    tableName = "grade_entries",
    foreignKeys = [
        ForeignKey(
            entity = GradeScaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["scaleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    primaryKeys = ["scaleId", "grade"],
    indices = [Index(value = ["scaleId"])]
)
data class GradeEntryEntity(
    val scaleId: Long,
    val grade: String, // e.g. "A+", "A"
    val points: Double // point representation on this scale (e.g. 4.0, 3.7)
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
