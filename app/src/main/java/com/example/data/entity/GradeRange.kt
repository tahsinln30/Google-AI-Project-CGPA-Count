package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grade_ranges")
data class GradeRange(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val systemId: Long,
    val grade: String,
    val minScore: Int,
    val maxScore: Int,
    val gradePoint: Double
)
