package com.example

import com.example.data.entity.Course
import com.example.data.entity.GradeRange
import com.example.ui.components.recalculateCourseGrade
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testRecalculateCourseGrade() {
    // School Level Grade Ranges
    val schoolRanges = listOf(
      GradeRange(systemId = 2, grade = "A", minScore = 90, maxScore = 100, gradePoint = 4.00),
      GradeRange(systemId = 2, grade = "B", minScore = 80, maxScore = 89, gradePoint = 3.00),
      GradeRange(systemId = 2, grade = "C", minScore = 70, maxScore = 79, gradePoint = 2.00),
      GradeRange(systemId = 2, grade = "D", minScore = 60, maxScore = 69, gradePoint = 1.00),
      GradeRange(systemId = 2, grade = "F", minScore = 0, maxScore = 59, gradePoint = 0.00)
    )

    // A course added with University Level "A+" (4.00)
    val courseAPlus = Course(
      id = 1,
      semesterId = 1,
      name = "Advanced Math",
      credits = 3.0,
      score = null,
      grade = "A+",
      gradePoint = 4.00
    )

    // Recalculating to School Level should map "A+" to "A" (since "A+" stripped is "A" which exists in School Level)
    val recalculatedAPlus = recalculateCourseGrade(courseAPlus, schoolRanges)
    assertEquals("A", recalculatedAPlus.grade)
    assertEquals(4.00, recalculatedAPlus.gradePoint, 0.001)

    // A course added with University Level "B-" (2.75)
    val courseBMinus = Course(
      id = 2,
      semesterId = 1,
      name = "Physics",
      credits = 4.0,
      score = null,
      grade = "B-",
      gradePoint = 2.75
    )

    // Recalculating to School Level should map "B-" to "B" (since "B-" stripped is "B" which exists in School Level)
    val recalculatedBMinus = recalculateCourseGrade(courseBMinus, schoolRanges)
    assertEquals("B", recalculatedBMinus.grade)
    assertEquals(3.00, recalculatedBMinus.gradePoint, 0.001)

    // A course with exact match "D" (2.00 under University)
    val courseD = Course(
      id = 3,
      semesterId = 1,
      name = "Chemistry",
      credits = 3.0,
      score = null,
      grade = "D",
      gradePoint = 2.00
    )

    // Recalculating to School Level should find exact match "D", which is 1.00 in School Level
    val recalculatedD = recalculateCourseGrade(courseD, schoolRanges)
    assertEquals("D", recalculatedD.grade)
    assertEquals(1.00, recalculatedD.gradePoint, 0.001)
  }
}

