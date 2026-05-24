package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.CourseEntity
import com.example.data.GradeEntryEntity
import com.example.data.GradeScaleWithEntries
import com.example.data.SemesterEntity
import com.example.data.SemesterWithCourses
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ComputedCourse(
    val entity: CourseEntity,
    val points: Double,
    val isValid: Boolean
)

data class ComputedSemester(
    val entity: SemesterEntity,
    val courses: List<ComputedCourse>,
    val gpa: Double,
    val totalCredits: Double,
    val validCredits: Double
)

data class GpaUiState(
    val semesters: List<ComputedSemester> = emptyList(),
    val activeScale: GradeScaleWithEntries? = null,
    val allScales: List<GradeScaleWithEntries> = emptyList(),
    val cgpa: Double = 0.0,
    val totalCredits: Double = 0.0,
    val cgpaMethod: String = "credits", // "credits" or "semester_weights"
    val isLoading: Boolean = true
)

class GpaViewModel(private val repository: AppRepository) : ViewModel() {

    init {
        // Trigger seeding in DB async on startup
        viewModelScope.launch {
            repository.seedInitialDataIfNecessary()
        }
    }

    val uiState: StateFlow<GpaUiState> = combine(
        repository.semestersWithCourses,
        repository.activeScaleWithEntries,
        repository.allGradeScalesWithEntries,
        repository.getSettingFlow("cgpa_method", "credits")
    ) { semesters, activeScale, allScales, cgpaMethod ->
        
        val computedSemesters = semesters.map { semWithCourses ->
            val computedCourses = semWithCourses.courses.map { course ->
                val entry = activeScale?.entries?.firstOrNull { it.grade.equals(course.grade, ignoreCase = true) }
                ComputedCourse(
                    entity = course,
                    points = entry?.points ?: 0.0,
                    isValid = entry != null
                )
            }
            
            val validCourses = computedCourses.filter { it.isValid }
            val sumCredits = validCourses.sumOf { it.entity.credits }
            val sumPointsCredits = validCourses.sumOf { it.entity.credits * it.points }
            val gpa = if (sumCredits > 0.0) sumPointsCredits / sumCredits else 0.0
            val totalCredits = semWithCourses.courses.sumOf { it.credits }
            
            ComputedSemester(
                entity = semWithCourses.semester,
                courses = computedCourses,
                gpa = gpa,
                totalCredits = totalCredits,
                validCredits = sumCredits
            )
        }
        
        var cgpa = 0.0
        var totalCreditsGlobal = 0.0
        
        if (cgpaMethod == "semester_weights") {
            val nonZeroSemesters = computedSemesters.filter { it.validCredits > 0.0 }
            val sumWeights = nonZeroSemesters.sumOf { it.entity.weight }
            val weightedGpasSum = nonZeroSemesters.sumOf { it.gpa * it.entity.weight }
            cgpa = if (sumWeights > 0.0) weightedGpasSum / sumWeights else 0.0
            totalCreditsGlobal = computedSemesters.sumOf { it.totalCredits }
        } else {
            // standard course-weighted (pool all valid courses together)
            var sumPoints = 0.0
            var sumCreditsNum = 0.0
            computedSemesters.forEach { sem ->
                sem.courses.filter { it.isValid }.forEach { course ->
                    sumPoints += course.entity.credits * course.points
                    sumCreditsNum += course.entity.credits
                }
            }
            cgpa = if (sumCreditsNum > 0.0) sumPoints / sumCreditsNum else 0.0
            totalCreditsGlobal = computedSemesters.sumOf { it.totalCredits }
        }
        
        GpaUiState(
            semesters = computedSemesters,
            activeScale = activeScale,
            allScales = allScales,
            cgpa = cgpa,
            totalCredits = totalCreditsGlobal,
            cgpaMethod = cgpaMethod,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GpaUiState()
    )

    fun addSemester(name: String, weight: Double = 1.0) {
        viewModelScope.launch {
            repository.insertSemester(name, weight)
        }
    }

    fun editSemester(id: Long, name: String, weight: Double) {
        viewModelScope.launch {
            repository.updateSemester(SemesterEntity(id = id, name = name, weight = weight))
        }
    }

    fun deleteSemester(id: Long) {
        viewModelScope.launch {
            repository.deleteSemester(id)
        }
    }

    fun addCourse(semesterId: Long, name: String, credits: Double, grade: String) {
        viewModelScope.launch {
            repository.insertCourse(semesterId, name, credits, grade)
        }
    }

    fun editCourse(courseId: Long, semesterId: Long, name: String, credits: Double, grade: String) {
        viewModelScope.launch {
            repository.updateCourse(CourseEntity(id = courseId, semesterId = semesterId, name = name, credits = credits, grade = grade))
        }
    }

    fun deleteCourse(id: Long) {
        viewModelScope.launch {
            repository.deleteCourse(id)
        }
    }

    fun setActiveScale(scaleId: Long) {
        viewModelScope.launch {
            repository.setActiveScale(scaleId)
        }
    }

    fun deleteGradeScale(scaleId: Long) {
        viewModelScope.launch {
            repository.deleteGradeScale(scaleId)
        }
    }

    fun addCustomScale(name: String, maxPoints: Double, entries: List<GradeEntryEntity>) {
        viewModelScope.launch {
            repository.saveCustomScale(name, maxPoints, entries)
        }
    }

    fun editCustomScale(scaleId: Long, name: String, maxPoints: Double, entries: List<GradeEntryEntity>) {
        viewModelScope.launch {
            repository.editCustomScale(scaleId, name, maxPoints, entries)
        }
    }

    fun setCgpaMethod(method: String) {
        viewModelScope.launch {
            repository.saveSetting("cgpa_method", method)
        }
    }

    class Factory(private val repository: AppRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GpaViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GpaViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
