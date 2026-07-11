package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalConfiguration
import com.example.R
import com.example.data.AppDatabase
import com.example.data.entity.Course
import com.example.data.entity.GradeRange
import com.example.data.entity.GradingSystem
import com.example.data.entity.Semester
import com.example.ui.AcademicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

sealed class AppScreen {
    object Dashboard : AppScreen()
    object SemesterDetail : AppScreen()
    object GradingScaleManager : AppScreen()
    object Settings : AppScreen()
    object PrivacyPolicy : AppScreen()
}

fun recalculateCourseGrade(course: Course, activeRanges: List<GradeRange>): Course {
    if (activeRanges.isEmpty()) return course

    // 1. Try to find exact match
    val exactMatch = activeRanges.firstOrNull { it.grade.equals(course.grade, ignoreCase = true) }
    if (exactMatch != null) {
        return course.copy(grade = exactMatch.grade, gradePoint = exactMatch.gradePoint)
    }

    // 2. Suffix stripping (e.g., "A+" -> "A", "B-" -> "B")
    if (course.grade.length > 1) {
        val stripped = course.grade.filter { it.isLetter() }
        val strippedMatch = activeRanges.firstOrNull { it.grade.equals(stripped, ignoreCase = true) }
        if (strippedMatch != null) {
            return course.copy(grade = strippedMatch.grade, gradePoint = strippedMatch.gradePoint)
        }
    }

    // 3. First letter match (e.g., if course grade is "A" but system only has "A+" and "A-", find first matching starting letter)
    val firstChar = course.grade.firstOrNull()
    if (firstChar != null && firstChar.isLetter()) {
        val letterMatch = activeRanges.firstOrNull { it.grade.startsWith(firstChar.toString(), ignoreCase = true) }
        if (letterMatch != null) {
            return course.copy(grade = letterMatch.grade, gradePoint = letterMatch.gradePoint)
        }
    }

    // 4. Default fallback: find the grade with the closest gradePoint
    val closestMatch = activeRanges.minByOrNull { Math.abs(it.gradePoint - course.gradePoint) }
    if (closestMatch != null) {
        return course.copy(grade = closestMatch.grade, gradePoint = closestMatch.gradePoint)
    }

    return course
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: AcademicViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Dashboard) }

    val semesters by viewModel.semesters.collectAsState()
    val gradingSystems by viewModel.gradingSystems.collectAsState()
    val activeGradingSystem by viewModel.activeGradingSystem.collectAsState()
    val activeGradeRanges by viewModel.activeGradeRanges.collectAsState()
    val allCourses by viewModel.allCourses.collectAsState()
    val selectedSemesterId by viewModel.selectedSemesterId.collectAsState()
    val selectedSemesterCourses by viewModel.selectedSemesterCourses.collectAsState()

    val recalculatedAllCourses = remember(allCourses, activeGradeRanges) {
        allCourses.map { recalculateCourseGrade(it, activeGradeRanges) }
    }
    val recalculatedSelectedCourses = remember(selectedSemesterCourses, activeGradeRanges) {
        selectedSemesterCourses.map { recalculateCourseGrade(it, activeGradeRanges) }
    }

    val activeSemester = semesters.find { it.id == selectedSemesterId }
    val isWide = LocalConfiguration.current.screenWidthDp >= 600

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!isWide && currentScreen != AppScreen.SemesterDetail) {
                NavigationBar(
                    modifier = Modifier.testTag("app_navigation_bar"),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.Dashboard,
                        onClick = { currentScreen = AppScreen.Dashboard },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        modifier = Modifier.testTag("nav_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.GradingScaleManager,
                        onClick = { currentScreen = AppScreen.GradingScaleManager },
                        icon = { Icon(Icons.Default.Grading, contentDescription = "Grading Scales") },
                        label = { Text("Scales", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        modifier = Modifier.testTag("nav_scales")
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.Settings,
                        onClick = { currentScreen = AppScreen.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        modifier = Modifier.testTag("nav_settings")
                    )
                    NavigationBarItem(
                        selected = currentScreen == AppScreen.PrivacyPolicy,
                        onClick = { currentScreen = AppScreen.PrivacyPolicy },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Privacy & Developer") },
                        label = { Text("Privacy", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        modifier = Modifier.testTag("nav_privacy")
                    )
                }
            }
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (isWide && currentScreen != AppScreen.SemesterDetail) {
                NavigationRail(
                    modifier = Modifier.testTag("app_navigation_rail"),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.Dashboard,
                        onClick = { currentScreen = AppScreen.Dashboard },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        modifier = Modifier.testTag("nav_dashboard")
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.GradingScaleManager,
                        onClick = { currentScreen = AppScreen.GradingScaleManager },
                        icon = { Icon(Icons.Default.Grading, contentDescription = "Grading Scales") },
                        label = { Text("Scales", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        modifier = Modifier.testTag("nav_scales")
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.Settings,
                        onClick = { currentScreen = AppScreen.Settings },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        modifier = Modifier.testTag("nav_settings")
                    )
                    NavigationRailItem(
                        selected = currentScreen == AppScreen.PrivacyPolicy,
                        onClick = { currentScreen = AppScreen.PrivacyPolicy },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Privacy & Developer") },
                        label = { Text("Privacy", style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                        modifier = Modifier.testTag("nav_privacy")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (isWide) Modifier.widthIn(max = 800.dp) else Modifier
                        )
                ) {
                    when (currentScreen) {
                        is AppScreen.Dashboard -> {
                            DashboardScreen(
                                semesters = semesters,
                                allCourses = recalculatedAllCourses,
                                activeGradingSystem = activeGradingSystem,
                                activeGradeRanges = activeGradeRanges,
                                onAddSemester = { name -> viewModel.addSemester(name) },
                                onUpdateSemester = { sem -> viewModel.updateSemester(sem) },
                                onDeleteSemester = { sem -> viewModel.deleteSemester(sem) },
                                onSelectSemester = { id ->
                                    viewModel.selectSemester(id)
                                    currentScreen = AppScreen.SemesterDetail
                                }
                            )
                        }
                        is AppScreen.SemesterDetail -> {
                            BackHandler {
                                viewModel.selectSemester(null)
                                currentScreen = AppScreen.Dashboard
                            }
                            if (activeSemester != null) {
                                SemesterDetailScreen(
                                    semester = activeSemester,
                                    courses = recalculatedSelectedCourses,
                                    activeGradingSystem = activeGradingSystem,
                                    activeGradeRanges = activeGradeRanges,
                                    onBack = {
                                        viewModel.selectSemester(null)
                                        currentScreen = AppScreen.Dashboard
                                    },
                                    onAddCourse = { name, credits, score, grade, gpa ->
                                        viewModel.addCourse(name, credits, score, grade, gpa)
                                    },
                                    onDeleteCourse = { course ->
                                        viewModel.deleteCourse(course)
                                    },
                                    viewModel = viewModel
                                )
                            } else {
                                currentScreen = AppScreen.Dashboard
                            }
                        }
                        is AppScreen.GradingScaleManager -> {
                            val sortedGradingSystems = remember(gradingSystems) {
                                gradingSystems.sortedWith(compareBy<GradingSystem> { system ->
                                    if (system.isSystemBuiltIn) {
                                        when {
                                            system.name.contains("University", ignoreCase = true) -> 1
                                            system.name.contains("College", ignoreCase = true) -> 2
                                            system.name.contains("School", ignoreCase = true) -> 3
                                            else -> 4
                                        }
                                    } else {
                                        5
                                    }
                                }.thenBy { it.id })
                            }
                            GradingScaleManagerScreen(
                                gradingSystems = sortedGradingSystems,
                                activeGradingSystem = activeGradingSystem,
                                onSelectActive = { id -> viewModel.selectActiveGradingSystem(id) },
                                onCreateCustomSystem = { name, ranges ->
                                    viewModel.createCustomGradingSystem(name, ranges)
                                },
                                onDeleteCustomSystem = { system ->
                                    viewModel.deleteCustomGradingSystem(system)
                                },
                                viewModel = viewModel
                            )
                        }
                        is AppScreen.Settings -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                semesters = semesters,
                                courses = allCourses
                            )
                        }
                        is AppScreen.PrivacyPolicy -> {
                            PrivacyPolicyScreen()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    semesters: List<Semester>,
    allCourses: List<Course>,
    activeGradingSystem: GradingSystem?,
    activeGradeRanges: List<GradeRange>,
    onAddSemester: (String) -> Unit,
    onUpdateSemester: (Semester) -> Unit,
    onDeleteSemester: (Semester) -> Unit,
    onSelectSemester: (Long) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var semesterToEdit by remember { mutableStateOf<Semester?>(null) }

    // Calculate Cumulative CGPA
    val totalCredits = allCourses.sumOf { it.credits }
    val totalWeightedPoints = allCourses.sumOf { it.credits * it.gradePoint }
    val cumulativeCgpa = if (totalCredits > 0.0) totalWeightedPoints / totalCredits else 0.00
    val maxGradePoint = activeGradeRanges.maxOfOrNull { it.gradePoint } ?: 4.00

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("semesters_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.size(80.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cgpa_count_logo_1783489118230),
                            contentDescription = "CGPA Count Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "CGPA Count",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "YOUR ACADEMIC RECORD KEEPER",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // CGPA Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = "Cumulative CGPA",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = String.format(Locale.US, "%.2f / %.2f", cumulativeCgpa, maxGradePoint),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text(
                                        text = "Semesters",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = semesters.size.toString(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Total Credits",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.1f", totalCredits),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Circular Gauge
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val strokeColor = MaterialTheme.colorScheme.primary
                            val emptyStrokeColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                            val gradientBrush = Brush.sweepGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.primary
                                )
                            )

                            Canvas(modifier = Modifier.size(100.dp)) {
                                drawCircle(
                                    color = emptyStrokeColor,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                                val progressAngle = (cumulativeCgpa / maxGradePoint).coerceIn(0.0, 1.0) * 360f
                                drawArc(
                                    brush = gradientBrush,
                                    startAngle = -90f,
                                    sweepAngle = progressAngle.toFloat(),
                                    useCenter = false,
                                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", (cumulativeCgpa / maxGradePoint * 100).coerceIn(0.0, 100.0)),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Academic Rating",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Semesters",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (activeGradingSystem != null) {
                        AssistChip(
                            onClick = {},
                            label = { Text(activeGradingSystem.name) },
                            leadingIcon = { Icon(Icons.Default.Grading, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                }
            }

            if (semesters.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No semesters added yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the '+' floating button to start tracking your academic CGPA.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(semesters) { semester ->
                    val semesterCourses = allCourses.filter { it.semesterId == semester.id }
                    val semCredits = semesterCourses.sumOf { it.credits }
                    val semWeighted = semesterCourses.sumOf { it.credits * it.gradePoint }
                    val semGpa = if (semCredits > 0.0) semWeighted / semCredits else 0.00

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSemester(semester.id) }
                            .testTag("semester_item_${semester.id}"),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = semester.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${semesterCourses.size} courses • $semCredits credits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (semGpa >= 3.50) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else if (semGpa >= 2.50) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.errorContainer
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "GPA: %.2f", semGpa),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = if (semGpa >= 3.50) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else if (semGpa >= 2.50) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                }

                                IconButton(
                                    onClick = { semesterToEdit = semester },
                                    modifier = Modifier.testTag("edit_semester_${semester.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Semester",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteSemester(semester) },
                                    modifier = Modifier.testTag("delete_semester_${semester.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Semester",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_semester_button"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Semester")
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Semester") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorText = ""
                        },
                        label = { Text("Semester Name") },
                        placeholder = { Text("e.g. Fall 2026, Year 1 Term 2") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("semester_name_input"),
                        isError = errorText.isNotEmpty()
                    )
                    if (errorText.isNotEmpty()) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.trim().isEmpty()) {
                            errorText = "Please enter a semester name"
                        } else {
                            onAddSemester(name.trim())
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_add_semester")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (semesterToEdit != null) {
        var name by remember(semesterToEdit) { mutableStateOf(semesterToEdit!!.name) }
        var errorText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { semesterToEdit = null },
            title = { Text("Edit Semester") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorText = ""
                        },
                        label = { Text("Semester Name") },
                        placeholder = { Text("e.g. Fall 2026, Year 1 Term 2") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_semester_name_input"),
                        isError = errorText.isNotEmpty()
                    )
                    if (errorText.isNotEmpty()) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.trim().isEmpty()) {
                            errorText = "Please enter a semester name"
                        } else {
                            onUpdateSemester(semesterToEdit!!.copy(name = name.trim()))
                            semesterToEdit = null
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_edit_semester")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { semesterToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SemesterDetailScreen(
    semester: Semester,
    courses: List<Course>,
    activeGradingSystem: GradingSystem?,
    activeGradeRanges: List<GradeRange>,
    onBack: () -> Unit,
    onAddCourse: (String, Double, Int?, String, Double) -> Unit,
    onDeleteCourse: (Course) -> Unit,
    viewModel: AcademicViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var courseToEdit by remember { mutableStateOf<Course?>(null) }

    // Calculate Semester GPA
    val semCredits = courses.sumOf { it.credits }
    val semWeighted = courses.sumOf { it.credits * it.gradePoint }
    val semGpa = if (semCredits > 0.0) semWeighted / semCredits else 0.00

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(semester.name) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("courses_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // GPA summary card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Semester GPA",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.2f", semGpa),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Credits",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.US, "%.1f", semCredits),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Enrolled Courses",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (courses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No courses added",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap the '+' button below to add your courses with letter grades.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(courses) { course ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("course_item_${course.id}"),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = course.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${course.credits} Credits",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (course.score != null) {
                                            Text(
                                                text = "Score: ${course.score}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clickable(enabled = false) {}
                                                .then(
                                                    Modifier.drawBehind {
                                                        drawCircle(
                                                            color = if (course.gradePoint >= 3.0) Color(0xFF10B981) else if (course.gradePoint >= 2.0) Color(0xFFF59E0B) else Color(0xFFEF4444),
                                                            alpha = 0.15f
                                                        )
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = course.grade,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = if (course.gradePoint >= 3.0) Color(0xFF059669) else if (course.gradePoint >= 2.0) Color(0xFFD97706) else Color(0xFFDC2626)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(Locale.US, "%.2f", course.gradePoint),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    IconButton(
                                        onClick = { courseToEdit = course },
                                        modifier = Modifier.testTag("edit_course_${course.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Course",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteCourse(course) },
                                        modifier = Modifier.testTag("delete_course_${course.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Course",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_course_button"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Course")
            }
        }
    }

    if (showAddDialog) {
        AddCourseDialog(
            activeGradingSystem = activeGradingSystem,
            activeGradeRanges = activeGradeRanges,
            viewModel = viewModel,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, credits, score, grade, gradePoint ->
                onAddCourse(name, credits, score, grade, gradePoint)
                showAddDialog = false
            }
        )
    }

    if (courseToEdit != null) {
        EditCourseDialog(
            course = courseToEdit!!,
            activeGradingSystem = activeGradingSystem,
            activeGradeRanges = activeGradeRanges,
            viewModel = viewModel,
            onDismiss = { courseToEdit = null },
            onConfirm = { name, credits, score, grade, gradePoint ->
                viewModel.updateCourse(
                    courseToEdit!!.copy(
                        name = name,
                        credits = credits,
                        score = score,
                        grade = grade,
                        gradePoint = gradePoint
                    )
                )
                courseToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseDialog(
    activeGradingSystem: GradingSystem?,
    activeGradeRanges: List<GradeRange>,
    viewModel: AcademicViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int?, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var creditsText by remember { mutableStateOf("") }

    // Direct selection of grade range index
    var selectedRangeIndex by remember { mutableStateOf(0) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_course_dialog"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Course",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Title / Code") },
                    placeholder = { Text("e.g. CSE-101, Calculus") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_name_input")
                )

                OutlinedTextField(
                    value = creditsText,
                    onValueChange = { creditsText = it },
                    label = { Text("Course Credits") },
                    placeholder = { Text("e.g. 3.0, 4.0, 1.5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("course_credits_input")
                )

                if (activeGradeRanges.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            val selectedRange = activeGradeRanges.getOrNull(selectedRangeIndex)
                            OutlinedTextField(
                                value = selectedRange?.let { "${it.grade} (GP: ${String.format(Locale.US, "%.2f", it.gradePoint)})" } ?: "Select Grade",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Grade Option") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("course_grade_dropdown_trigger")
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                activeGradeRanges.forEachIndexed { index, range ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${range.grade}  (GP: ${String.format(Locale.US, "%.2f", range.gradePoint)})",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        },
                                        onClick = {
                                            selectedRangeIndex = index
                                            dropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("grade_option_${range.grade}")
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No grade options found. Please configure a scale in Scales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val nameTrim = name.trim()
                            val creditsVal = creditsText.toDoubleOrNull()

                            if (nameTrim.isEmpty()) {
                                errorMsg = "Course title is required"
                                return@Button
                            }
                            if (creditsVal == null || creditsVal <= 0.0) {
                                errorMsg = "Valid course credits are required"
                                return@Button
                            }

                            val selectedRange = activeGradeRanges.getOrNull(selectedRangeIndex)
                            if (selectedRange == null) {
                                errorMsg = "Invalid grade selection"
                                return@Button
                            }
                            onConfirm(
                                nameTrim,
                                creditsVal,
                                null, // No numerical score is collected now as it's strictly option basis
                                selectedRange.grade,
                                selectedRange.gradePoint
                            )
                        },
                        modifier = Modifier.testTag("dialog_confirm_add_course")
                    ) {
                        Text("Add Course")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCourseDialog(
    course: Course,
    activeGradingSystem: GradingSystem?,
    activeGradeRanges: List<GradeRange>,
    viewModel: AcademicViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int?, String, Double) -> Unit
) {
    var name by remember(course) { mutableStateOf(course.name) }
    var creditsText by remember(course) { mutableStateOf(String.format(Locale.US, "%.1f", course.credits)) }

    // Direct selection of grade range index
    var selectedRangeIndex by remember(course, activeGradeRanges) {
        mutableStateOf(
            activeGradeRanges.indexOfFirst { it.grade == course.grade }.coerceAtLeast(0)
        )
    }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("edit_course_dialog"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Course",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Title / Code") },
                    placeholder = { Text("e.g. CSE-101, Calculus") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_course_name_input")
                )

                OutlinedTextField(
                    value = creditsText,
                    onValueChange = { creditsText = it },
                    label = { Text("Course Credits") },
                    placeholder = { Text("e.g. 3.0, 4.0, 1.5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_course_credits_input")
                )

                if (activeGradeRanges.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                        ) {
                            val selectedRange = activeGradeRanges.getOrNull(selectedRangeIndex)
                            OutlinedTextField(
                                value = selectedRange?.let { "${it.grade} (GP: ${String.format(Locale.US, "%.2f", it.gradePoint)})" } ?: "Select Grade",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Grade Option") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("edit_course_grade_dropdown_trigger")
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                activeGradeRanges.forEachIndexed { index, range ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${range.grade}  (GP: ${String.format(Locale.US, "%.2f", range.gradePoint)})",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        },
                                        onClick = {
                                            selectedRangeIndex = index
                                            dropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("edit_grade_option_${range.grade}")
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No grade options found. Please configure a scale in Scales.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val nameTrim = name.trim()
                            val creditsVal = creditsText.toDoubleOrNull()

                            if (nameTrim.isEmpty()) {
                                errorMsg = "Course title is required"
                                return@Button
                            }
                            if (creditsVal == null || creditsVal <= 0.0) {
                                errorMsg = "Valid course credits are required"
                                return@Button
                            }

                            val selectedRange = activeGradeRanges.getOrNull(selectedRangeIndex)
                            if (selectedRange == null) {
                                errorMsg = "Invalid grade selection"
                                return@Button
                            }
                            onConfirm(
                                nameTrim,
                                creditsVal,
                                null,
                                selectedRange.grade,
                                selectedRange.gradePoint
                            )
                        },
                        modifier = Modifier.testTag("dialog_confirm_edit_course")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun GradingScaleManagerScreen(
    gradingSystems: List<GradingSystem>,
    activeGradingSystem: GradingSystem?,
    onSelectActive: (Long) -> Unit,
    onCreateCustomSystem: (String, List<GradeRange>) -> Unit,
    onDeleteCustomSystem: (GradingSystem) -> Unit,
    viewModel: AcademicViewModel
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingSystem by remember { mutableStateOf<GradingSystem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Grading Scales",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Choose which academic grading scale fits your school/university, or design your own.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Available Scales",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.testTag("open_create_scale_dialog")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Scale")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .testTag("scales_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(gradingSystems) { system ->
                val isActive = system.id == activeGradingSystem?.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scale_item_${system.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isActive) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = system.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (system.isSystemBuiltIn) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Built-in",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row {
                                if (!isActive) {
                                    TextButton(
                                        onClick = { onSelectActive(system.id) },
                                        modifier = Modifier.testTag("set_active_scale_${system.id}")
                                    ) {
                                        Text("Set Active")
                                    }
                                } else {
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("Active") },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            labelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }

                                 IconButton(
                                    onClick = { editingSystem = system },
                                    modifier = Modifier.testTag("edit_scale_${system.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Scale",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteCustomSystem(system) },
                                    enabled = !isActive,
                                    modifier = Modifier.testTag("delete_scale_${system.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Scale",
                                        tint = if (isActive) {
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        } else {
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        }
                                    )
                                }
                            }
                        }

                        // Let's list a compact preview of ranges
                        ScaleRangesPreview(systemId = system.id, viewModel = viewModel)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCustomScaleDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, ranges ->
                onCreateCustomSystem(name, ranges)
                showCreateDialog = false
            }
        )
    }

    if (editingSystem != null) {
        val ranges by viewModel.getGradeRangesForSystemFlow(editingSystem!!.id).collectAsState(initial = emptyList())
        if (ranges.isNotEmpty()) {
            CreateCustomScaleDialog(
                initialSystem = editingSystem,
                initialRanges = ranges,
                onDismiss = { editingSystem = null },
                onConfirm = { name, newRanges ->
                    viewModel.updateCustomGradingSystem(editingSystem!!.copy(name = name), newRanges)
                    editingSystem = null
                }
            )
        }
    }
}

@Composable
fun ScaleRangesPreview(systemId: Long, viewModel: AcademicViewModel) {
    val ranges by viewModel.getGradeRangesForSystemFlow(systemId).collectAsState(initial = emptyList())

    if (ranges.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(8.dp))
        // Show compact scrollable row list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ranges.forEach { range ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = range.grade,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = String.format(Locale.US, "%.1f", range.gradePoint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateCustomScaleDialog(
    initialSystem: GradingSystem? = null,
    initialRanges: List<GradeRange> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, List<GradeRange>) -> Unit
) {
    var systemName by remember { mutableStateOf(initialSystem?.name ?: "") }
    val customRanges = remember { mutableStateListOf<GradeRange>() }

    // State for temporary add
    var tempGrade by remember { mutableStateOf("") }
    var tempGradePoint by remember { mutableStateOf("") }

    var errorMsg by remember { mutableStateOf("") }

    // Pre-populate with default layout templates to make it easier for the user!
    LaunchedEffect(Unit) {
        if (customRanges.isEmpty()) {
            if (initialRanges.isNotEmpty()) {
                customRanges.addAll(initialRanges)
            } else {
                customRanges.addAll(
                    listOf(
                        GradeRange(systemId = 0, grade = "A", minScore = 0, maxScore = 0, gradePoint = 4.0),
                        GradeRange(systemId = 0, grade = "B", minScore = 0, maxScore = 0, gradePoint = 3.0),
                        GradeRange(systemId = 0, grade = "C", minScore = 0, maxScore = 0, gradePoint = 2.0),
                        GradeRange(systemId = 0, grade = "F", minScore = 0, maxScore = 0, gradePoint = 0.0)
                    )
                )
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp)
                .testTag("create_scale_dialog"),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = if (initialSystem != null) "Edit Grading Scale" else "Create Custom Scale",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = systemName,
                    onValueChange = { systemName = it },
                    label = { Text("Scale Name") },
                    placeholder = { Text("e.g. My University Scale, 5.0 Scale") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scale_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Define Custom Grade Options & Grade Points",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )

                // Quick Add Fields (Spacious 2-column layout with short labels)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tempGrade,
                        onValueChange = { tempGrade = it },
                        label = { Text("Grade") },
                        placeholder = { Text("A+") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("quick_grade_input")
                    )
                    OutlinedTextField(
                        value = tempGradePoint,
                        onValueChange = { tempGradePoint = it },
                        label = { Text("GP") },
                        placeholder = { Text("4.00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("quick_gp_input")
                    )
                    IconButton(
                        onClick = {
                            val gpVal = tempGradePoint.toDoubleOrNull()
                            val gradeVal = tempGrade.trim()

                            if (gradeVal.isEmpty() || gpVal == null) {
                                errorMsg = "Both Grade and Grade Point fields must be filled correctly"
                                return@IconButton
                            }

                            customRanges.add(
                                GradeRange(
                                    systemId = 0,
                                    grade = gradeVal,
                               minScore = 0,
                                    maxScore = 0,
                                    gradePoint = gpVal
                                )
                            )
                            tempGrade = ""
                            tempGradePoint = ""
                            errorMsg = ""
                        },
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("quick_add_range_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Range",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                if (errorMsg.isNotEmpty()) {
                    Text(
                        text = errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // List of current ranges
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customRanges) { range ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = range.grade,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "Grade Point",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%.2f", range.gradePoint),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { customRanges.remove(range) }) {
                                Icon(Icons.Default.RemoveCircle, contentDescription = "Delete Range", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (systemName.trim().isEmpty()) {
                                errorMsg = "Scale Name cannot be empty"
                                return@Button
                            }
                            if (customRanges.isEmpty()) {
                                errorMsg = "At least one grade range must be defined"
                                return@Button
                            }
                            onConfirm(systemName.trim(), customRanges.toList())
                        },
                        modifier = Modifier.testTag("dialog_confirm_create_scale")
                    ) {
                        Text(if (initialSystem != null) "Save Scale" else "Create Scale")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: AcademicViewModel,
    semesters: List<Semester>,
    courses: List<Course>
) {
    val darkModePref by viewModel.darkModePref.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        // Theme Toggle Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Dark Mode Preference",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Toggles automatically based on system preferences or locks to light/dark themes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                        val selected = darkModePref == mode
                        val label = when (mode) {
                            "SYSTEM" -> "System Default"
                            "LIGHT" -> "Light"
                            else -> "Dark"
                        }
                        val icon = when (mode) {
                            "SYSTEM" -> Icons.Default.Settings
                            "LIGHT" -> Icons.Default.LightMode
                            else -> Icons.Default.DarkMode
                        }

                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setDarkMode(mode) },
                            label = { 
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = if (mode == "SYSTEM") 10.sp else 11.sp
                                )
                            },
                            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(13.dp)) },
                            modifier = Modifier
                                .weight(if (mode == "SYSTEM") 1.3f else 1f)
                                .testTag("theme_chip_$mode")
                        )
                    }
                }
            }
        }

        // Stats Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Data Summary",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Semesters Saved", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = semesters.size.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Courses Tracked", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = courses.size.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Danger Zone Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Permanently wipe all application data, courses, semesters, and custom scales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reset All Application Data")
                }
            }
        }
    }

    if (showResetDialog) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Wipe All Data?") },
            text = { Text("This action cannot be undone. All your custom semesters, grade logs, and custom grading systems will be deleted permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_reset_data")
                ) {
                    Text("Yes, Wipe Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PrivacyPolicyScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("privacy_policy_page"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cgpa_count_logo_1783489118230),
                        contentDescription = "CGPA Count Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "CGPA Count",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "v2.00",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Developer Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .then(
                                Modifier.drawBehind {
                                    drawCircle(color = Color(0xFF3B82F6).copy(alpha = 0.2f))
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TA",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Tahsin Ahmed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Developer of CGPA Count",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Privacy Guarantee Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("privacy_policy_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy & Policy Guidelines",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text(
                        text = "Your privacy is our core commitment. Academic records represent highly sensitive personal milestones. Here is exactly how your data is secured inside the CGPA Count app:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    BulletPoint(text = "All scores, course credentials, credit counts, and semester listings are saved exclusively on your local physical device inside a secure Room Database container.")
                    BulletPoint(text = "Absolutely zero academic telemetry, analytics data, or scores are transferred, synced, or exposed to third-party databases, clouds, or APIs.")
                    BulletPoint(text = "Since the database lives completely offline in your local sandbox, deleting the application permanently wipes all associated academic logs immediately.")
                    BulletPoint(text = "The application does not request unnecessary background, network, hardware, or location permissions, keeping execution perfectly sandboxed.")

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Developed by Tahsin Ahmed under high-security guidelines to guarantee student privacy.",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
