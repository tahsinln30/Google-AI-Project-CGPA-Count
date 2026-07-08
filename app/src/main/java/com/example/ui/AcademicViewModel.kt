package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.Course
import com.example.data.entity.GradeRange
import com.example.data.entity.GradingSystem
import com.example.data.entity.Semester
import com.example.data.repository.AcademicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class AcademicViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = AcademicRepository(database.academicDao())

    // All semesters
    val semesters: StateFlow<List<Semester>> = repository.allSemesters
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All grading systems
    val gradingSystems: StateFlow<List<GradingSystem>> = repository.allGradingSystems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active/Default grading system
    val activeGradingSystem: StateFlow<GradingSystem?> = repository.defaultGradingSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Grade ranges for active system
    val activeGradeRanges: StateFlow<List<GradeRange>> = activeGradingSystem
        .flatMapLatest { system ->
            if (system != null) {
                repository.getGradeRangesForSystemFlow(system.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All courses
    val allCourses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dark Mode Preference (SYSTEM, LIGHT, DARK)
    private val prefs = application.getSharedPreferences("cgpa_count_prefs", Context.MODE_PRIVATE)
    private val _darkModePref = MutableStateFlow(prefs.getString("dark_mode", "SYSTEM") ?: "SYSTEM")
    val darkModePref: StateFlow<String> = _darkModePref.asStateFlow()

    fun setDarkMode(mode: String) {
        prefs.edit().putString("dark_mode", mode).apply()
        _darkModePref.value = mode
    }

    // Selected Semester (for drill-down)
    private val _selectedSemesterId = MutableStateFlow<Long?>(null)
    val selectedSemesterId: StateFlow<Long?> = _selectedSemesterId.asStateFlow()

    val selectedSemesterCourses: StateFlow<List<Course>> = _selectedSemesterId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getCoursesForSemester(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectSemester(id: Long?) {
        _selectedSemesterId.value = id
    }

    // Semester Operations
    fun addSemester(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSemester(Semester(name = name))
        }
    }

    fun deleteSemester(semester: Semester) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_selectedSemesterId.value == semester.id) {
                _selectedSemesterId.value = null
            }
            repository.deleteSemester(semester)
        }
    }

    // Course Operations
    fun addCourse(name: String, credits: Double, score: Int?, grade: String, gradePoint: Double) {
        val semesterId = _selectedSemesterId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCourse(
                Course(
                    semesterId = semesterId,
                    name = name,
                    credits = credits,
                    score = score,
                    grade = grade,
                    gradePoint = gradePoint
                )
            )
        }
    }

    fun updateCourse(course: Course) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCourse(course)
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCourse(course)
        }
    }

    // Grading System Operations
    fun createCustomGradingSystem(name: String, ranges: List<GradeRange>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGradingSystem(
                GradingSystem(name = name, isDefault = false, isSystemBuiltIn = false),
                ranges
            )
        }
    }

    fun updateCustomGradingSystem(system: GradingSystem, ranges: List<GradeRange>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateGradingSystem(system, ranges)
        }
    }

    fun deleteCustomGradingSystem(system: GradingSystem) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentActive = activeGradingSystem.value
            if (currentActive?.id == system.id) {
                // Find a built-in or alternative scale
                val systems = gradingSystems.value
                val fallback = systems.firstOrNull { it.isSystemBuiltIn } ?: systems.firstOrNull { it.id != system.id }
                if (fallback != null) {
                    repository.setDefaultGradingSystem(fallback.id)
                }
            }
            repository.deleteGradingSystem(system)
        }
    }

    fun selectActiveGradingSystem(systemId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDefaultGradingSystem(systemId)
        }
    }

    fun getGradeRangesForSystemFlow(systemId: Long): Flow<List<GradeRange>> {
        return repository.getGradeRangesForSystemFlow(systemId)
    }

    // Synchronous/helper to calculate grade details from score
    suspend fun getGradeDetailsForScore(score: Int, systemId: Long): Pair<String, Double>? {
        return withContext(Dispatchers.IO) {
            val ranges = repository.getGradeRangesForSystem(systemId)
            val matched = ranges.firstOrNull { score in it.minScore..it.maxScore }
            if (matched != null) {
                Pair(matched.grade, matched.gradePoint)
            } else {
                null
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
            val dao = database.academicDao()
            
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
            
            _selectedSemesterId.value = null
        }
    }
}
