package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.data.CourseEntity
import com.example.data.GradeEntryEntity
import com.example.data.GradeScaleWithEntries
import com.example.data.SemesterEntity
import java.util.Locale

// Dialog Management Sealed Interface
sealed interface ActiveDialog {
    object None : ActiveDialog
    object AddSemester : ActiveDialog
    data class EditSemester(val semester: SemesterEntity) : ActiveDialog
    data class AddCourse(val semesterId: Long) : ActiveDialog
    data class EditCourse(val course: CourseEntity) : ActiveDialog
    object ScalesManagement : ActiveDialog
    object AddCustomScale : ActiveDialog
    data class EditCustomScale(val scale: GradeScaleWithEntries) : ActiveDialog
    object AppSettings : ActiveDialog
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GpaAppScreen(viewModel: GpaViewModel) {
    val state by viewModel.uiState.collectAsState()
    var activeDialog by remember { mutableStateOf<ActiveDialog>(ActiveDialog.None) }

    // Navigation and Status bars margins in Compose
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "Academic School Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CGPA Count",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { activeDialog = ActiveDialog.ScalesManagement },
                        modifier = Modifier.testTag("scales_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "GPA Scales Configurations",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { activeDialog = ActiveDialog.AppSettings },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "App Settings and Reset"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                windowInsets = WindowInsets.statusBars
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { activeDialog = ActiveDialog.AddSemester },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("add_semester_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Semester")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Semester", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
                contentPadding = WindowInsets.navigationBars.asPaddingValues()
            ) {
                // Main visual summary gauge card
                item {
                    CgpaHeaderGauge(state)
                }

                // If empty state, show nice educational display
                if (state.semesters.isEmpty()) {
                    item {
                        EmptySemestersState(onAddClick = { activeDialog = ActiveDialog.AddSemester })
                    }
                } else {
                    // Semesters list
                    items(state.semesters, key = { it.entity.id }) { computedSemester ->
                        SemesterCardItem(
                            computedSemester = computedSemester,
                            activeScale = state.activeScale,
                            onAddCourseClick = { activeDialog = ActiveDialog.AddCourse(semesterId = computedSemester.entity.id) },
                            onEditSemesterClick = { activeDialog = ActiveDialog.EditSemester(semester = computedSemester.entity) },
                            onDeleteSemesterClick = { viewModel.deleteSemester(computedSemester.entity.id) },
                            onEditCourseClick = { activeDialog = ActiveDialog.EditCourse(course = it) },
                            onDeleteCourseClick = { viewModel.deleteCourse(it.id) }
                        )
                    }
                }

                // Spacer at bottom to guarantee visual boundary clear of the FAB
                item {
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
        }
    }

    // Modal Dialog Manager Actions
    when (val dialog = activeDialog) {
        is ActiveDialog.None -> {}
        is ActiveDialog.AddSemester -> {
            AddEditSemesterDialog(
                isEdit = false,
                onDismiss = { activeDialog = ActiveDialog.None },
                onConfirm = { name, weight ->
                    viewModel.addSemester(name, weight)
                    activeDialog = ActiveDialog.None
                }
            )
        }
        is ActiveDialog.EditSemester -> {
            AddEditSemesterDialog(
                isEdit = true,
                initialName = dialog.semester.name,
                initialWeight = dialog.semester.weight,
                onDismiss = { activeDialog = ActiveDialog.None },
                onConfirm = { name, weight ->
                    viewModel.editSemester(dialog.semester.id, name, weight)
                    activeDialog = ActiveDialog.None
                }
            )
        }
        is ActiveDialog.AddCourse -> {
            AddEditCourseDialog(
                isEdit = false,
                activeScale = state.activeScale,
                onDismiss = { activeDialog = ActiveDialog.None },
                onConfirm = { name, credits, grade ->
                    viewModel.addCourse(dialog.semesterId, name, credits, grade)
                    activeDialog = ActiveDialog.None
                }
            )
        }
        is ActiveDialog.EditCourse -> {
            AddEditCourseDialog(
                isEdit = true,
                activeScale = state.activeScale,
                initialName = dialog.course.name,
                initialCredits = dialog.course.credits,
                initialGrade = dialog.course.grade,
                onDismiss = { activeDialog = ActiveDialog.None },
                onConfirm = { name, credits, grade ->
                    viewModel.editCourse(
                        courseId = dialog.course.id,
                        semesterId = dialog.course.semesterId,
                        name = name,
                        credits = credits,
                        grade = grade
                    )
                    activeDialog = ActiveDialog.None
                }
            )
        }
        is ActiveDialog.ScalesManagement -> {
            ScalesManagementDialog(
                state = state,
                onDismiss = { activeDialog = ActiveDialog.None },
                onSelectActive = { scaleId -> viewModel.setActiveScale(scaleId) },
                onDeleteScale = { scaleId -> viewModel.deleteGradeScale(scaleId) },
                onAddScaleClick = { activeDialog = ActiveDialog.AddCustomScale },
                onEditScaleClick = { scale -> activeDialog = ActiveDialog.EditCustomScale(scale) }
            )
        }
        is ActiveDialog.AddCustomScale -> {
            CreateEditScaleDialog(
                isEdit = false,
                onDismiss = { activeDialog = ActiveDialog.ScalesManagement },
                onConfirm = { name, maxPoints, entries ->
                    viewModel.addCustomScale(name, maxPoints, entries)
                    activeDialog = ActiveDialog.ScalesManagement
                }
            )
        }
        is ActiveDialog.EditCustomScale -> {
            CreateEditScaleDialog(
                isEdit = true,
                scaleId = dialog.scale.scale.id,
                initialName = dialog.scale.scale.name,
                initialMaxPoints = dialog.scale.scale.maxPoints,
                initialEntries = dialog.scale.entries,
                onDismiss = { activeDialog = ActiveDialog.ScalesManagement },
                onConfirmWithId = { id, name, maxPoints, entries ->
                    if (id != null) {
                        viewModel.editCustomScale(id, name, maxPoints, entries)
                    }
                    activeDialog = ActiveDialog.ScalesManagement
                }
            )
        }
        is ActiveDialog.AppSettings -> {
            AppSettingsDialog(
                cgpaMethod = state.cgpaMethod,
                onDismiss = { activeDialog = ActiveDialog.None },
                onMethodChange = { viewModel.setCgpaMethod(it) }
            )
        }
    }
}

// -------------------------------------------------------------
// Beautiful visual CGPA Gauge Header
// -------------------------------------------------------------
@Composable
fun CgpaHeaderGauge(state: GpaUiState) {
    val activeScaleName = state.activeScale?.scale?.name ?: "No Scale Active"
    val maxPoints = state.activeScale?.scale?.maxPoints ?: 4.0
    val formattedCgpa = String.format(Locale.getDefault(), "%.2f", state.cgpa)
    val formattedCredits = String.format(Locale.getDefault(), "%.1f", state.totalCredits)
    val displayMethod = if (state.cgpaMethod == "semester_weights") "Semester-Weighted" else "Total-Course-Weighted"

    // Responsive adaptation layout
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .widthIn(max = 650.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Information Side
            Column(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "CUMULATIVE CGPA",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formattedCgpa,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("gpa_score")
                    )
                    Text(
                        text = " / $maxPoints",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))
                
                // Key-value information chips/rows
                InfoRowLabel(label = "Active Scheme", value = activeScaleName)
                Spacer(modifier = Modifier.height(4.dp))
                InfoRowLabel(label = "Completed Credits", value = "$formattedCredits Credits")
                Spacer(modifier = Modifier.height(4.dp))
                InfoRowLabel(label = "Weighting Rule", value = displayMethod)
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Beautiful Canvas Radial Gauge
            val isDark = MaterialTheme.colorScheme.primary.red < 0.5f // simplistic check
            val trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
            val fillArcColor = MaterialTheme.colorScheme.primary

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .weight(0.8f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw outer track
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = 12.dp.toPx())
                    )
                    // Draw dynamic color arc
                    val ratioValue = if (maxPoints > 0.0 && !state.cgpa.isNaN()) (state.cgpa / maxPoints) else 0.0
                    val coercedRatio = if (ratioValue.isNaN() || ratioValue.isInfinite()) 0.0 else ratioValue.coerceIn(0.0, 1.0)
                    drawArc(
                        color = fillArcColor,
                        startAngle = -220f,
                        sweepAngle = coercedRatio.toFloat() * 260f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val rawPercentValue = if (maxPoints > 0.0 && !state.cgpa.isNaN()) (state.cgpa / maxPoints) * 100.0 else 0.0
                    val progressPercent = if (rawPercentValue.isNaN() || rawPercentValue.isInfinite()) 0.0 else rawPercentValue.coerceIn(0.0, 100.0)
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f%%", progressPercent),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRowLabel(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

// -------------------------------------------------------------
// Interactive Semester List Items
// -------------------------------------------------------------
@Composable
fun SemesterCardItem(
    computedSemester: ComputedSemester,
    activeScale: GradeScaleWithEntries?,
    onAddCourseClick: () -> Unit,
    onEditSemesterClick: () -> Unit,
    onDeleteSemesterClick: () -> Unit,
    onEditCourseClick: (CourseEntity) -> Unit,
    onDeleteCourseClick: (CourseEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val averageGpa = String.format(Locale.getDefault(), "%.2f", computedSemester.gpa)
    val totalCredits = String.format(Locale.getDefault(), "%.1f", computedSemester.totalCredits)
    val sWeight = String.format(Locale.getDefault(), "%.2f", computedSemester.entity.weight)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .widthIn(max = 650.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = borderStrokeForGpa(computedSemester.gpa, activeScale?.scale?.maxPoints ?: 4.0)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Semester Title, Action Controls, and Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = computedSemester.entity.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "W: $sWeight",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$totalCredits Credits completes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = averageGpa,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = colorForGpa(computedSemester.gpa, activeScale?.scale?.maxPoints ?: 4.0)
                        )
                        Text(
                            text = "GPA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Edit/Delete Settings
                    var menuExpanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Semester Options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.Edit, "Edit") },
                                text = { Text("Edit Semester") },
                                onClick = {
                                    menuExpanded = false
                                    onEditSemesterClick()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Filled.Delete, "Delete", tint = Color.Red) },
                                text = { Text("Delete Semester", color = Color.Red) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteSemesterClick()
                                }
                            )
                        }
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (computedSemester.courses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No Courses Added Yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = onAddCourseClick,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(Icons.Filled.Add, "Add Course")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add First Course")
                                }
                            }
                        }
                    } else {
                        // Table/List Header for Courses
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Course Title",
                                modifier = Modifier.weight(2f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Credits",
                                modifier = Modifier.weight(0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Grade",
                                modifier = Modifier.weight(0.7f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Actions",
                                modifier = Modifier.weight(0.8f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        computedSemester.courses.forEach { computedCourse ->
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(2f)) {
                                    Text(
                                        text = computedCourse.entity.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Text(
                                    text = String.format(Locale.getDefault(), "%.1f", computedCourse.entity.credits),
                                    modifier = Modifier.weight(0.7f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (computedCourse.isValid) {
                                                    colorForGpa(computedCourse.points, activeScale?.scale?.maxPoints ?: 4.0).copy(alpha = 0.15f)
                                                } else {
                                                    MaterialTheme.colorScheme.errorContainer
                                                },
                                                RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (computedCourse.isValid) {
                                                    colorForGpa(computedCourse.points, activeScale?.scale?.maxPoints ?: 4.0).copy(alpha = 0.5f)
                                                } else {
                                                    MaterialTheme.colorScheme.error
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = computedCourse.entity.grade,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (computedCourse.isValid) {
                                                colorForGpa(computedCourse.points, activeScale?.scale?.maxPoints ?: 4.0)
                                            } else {
                                                MaterialTheme.colorScheme.error
                                            }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.weight(0.8f),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onEditCourseClick(computedCourse.entity) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit Course",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { onDeleteCourseClick(computedCourse.entity) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete Course",
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAddCourseClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Filled.Add, "Add Course", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Course", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// Visual color rules depending on the GPA Range
@Composable
fun colorForGpa(gpa: Double, maxPoints: Double): Color {
    val rawRatio = if (maxPoints > 0.0 && !gpa.isNaN()) gpa / maxPoints else 0.0
    val ratio = if (rawRatio.isNaN() || rawRatio.isInfinite()) 0.0 else rawRatio.coerceIn(0.0, 1.0)
    return when {
        ratio >= 0.85 -> Color(0xFF10B981) // Gorgeous Green
        ratio >= 0.70 -> Color(0xFF3B82F6) // Warm Blue
        ratio >= 0.50 -> Color(0xFFF59E0B) // Amber warning
        else -> Color(0xFFEF4444) // Bright Red Fail
    }
}

@Composable
fun borderStrokeForGpa(gpa: Double, maxPoints: Double): androidx.compose.foundation.BorderStroke? {
    if (gpa == 0.0) return null
    return androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = colorForGpa(gpa, maxPoints).copy(alpha = 0.25f)
    )
}

// -------------------------------------------------------------
// Empty semesters screen component
// -------------------------------------------------------------
@Composable
fun EmptySemestersState(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .widthIn(max = 650.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.School,
                contentDescription = "School hat logo",
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Welcome to CGPA Count!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Keep track of semesters, customized credit weights, and calculate your ultimate grade standings in a secure database workspace.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("on_add_init_semester")
            ) {
                Icon(Icons.Filled.Add, "Add Semester")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Your First Semester", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// -------------------------------------------------------------
// Core Add / Edit Semester Dialog
// -------------------------------------------------------------
@Composable
fun AddEditSemesterDialog(
    isEdit: Boolean,
    initialName: String = "",
    initialWeight: Double = 1.0,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var weightString by remember { mutableStateOf(initialWeight.toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Semester" else "Add Semester", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Semester Name") },
                    placeholder = { Text("e.g. Fall 2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_semester_name_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = weightString,
                    onValueChange = { weightString = it },
                    label = { Text("Custom Semester Weight") },
                    placeholder = { Text("e.g. 1.0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("add_semester_weight_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Optional multiplier for CGPA under Semester-Weighted rule. Standard default is 1.0.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Semester name cannot be empty."
                        return@Button
                    }
                    val weight = weightString.toDoubleOrNull()
                    if (weight == null || weight <= 0.0) {
                        errorMsg = "Weight must be an alphanumeric positive number."
                        return@Button
                    }
                    onConfirm(name, weight)
                },
                modifier = Modifier.testTag("confirm_semester_save")
            ) {
                Text("Save")
            }
        }
    )
}

// -------------------------------------------------------------
// Core Add / Edit Course Dialog
// -------------------------------------------------------------
@Composable
fun AddEditCourseDialog(
    isEdit: Boolean,
    activeScale: GradeScaleWithEntries?,
    initialName: String = "",
    initialCredits: Double = 3.0,
    initialGrade: String = "A",
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var creditsString by remember { mutableStateOf(if (isEdit) initialCredits.toString() else "3.0") }
    var grade by remember { mutableStateOf(initialGrade) }
    var customGradeEnabled by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Dropdown list based on registered Scale entries
    val entries = activeScale?.entries?.sortedByDescending { it.points } ?: emptyList()
    var dropdownExpanded by remember { var expandedState = false; mutableStateOf(expandedState) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Course" else "Add Course", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Title") },
                    placeholder = { Text("e.g. Applied Physics") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("course_name_input")
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = creditsString,
                    onValueChange = { creditsString = it },
                    label = { Text("Course Credits / Weights") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("course_credits_input")
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (customGradeEnabled || entries.isEmpty()) {
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it.uppercase() },
                        label = { Text("Grade Letter") },
                        placeholder = { Text("A+, B, C etc") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("course_grade_text_input")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = { customGradeEnabled = false }) {
                        Text("Use Predefined Active Scale Grades")
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = grade,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grade Letter") },
                            modifier = Modifier.fillMaxWidth().clickable { dropdownExpanded = true },
                            trailingIcon = {
                                IconButton(onClick = { dropdownExpanded = true }) {
                                    Icon(Icons.Filled.KeyboardArrowDown, "Grade select")
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 240.dp)
                        ) {
                            entries.forEach { entry ->
                                DropdownMenuItem(
                                    text = { Text("${entry.grade} (${entry.points} pts)") },
                                    onClick = {
                                        grade = entry.grade
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = { customGradeEnabled = true }) {
                        Text("Enter Custom Grade Overrides")
                    }
                }

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Course title is mandatory."
                        return@Button
                    }
                    val credits = creditsString.toDoubleOrNull()
                    if (credits == null || credits <= 0) {
                        errorMsg = "Credits must be a valid positive decimal quantity."
                        return@Button
                    }
                    if (grade.isBlank()) {
                        errorMsg = "Grade value must capture school scale code (A, B...)."
                        return@Button
                    }
                    onConfirm(name, credits, grade)
                },
                modifier = Modifier.testTag("confirm_course_save")
            ) {
                Text("Save")
            }
        }
    )
}

// -------------------------------------------------------------
// Grading scales management dashboard dialog
// -------------------------------------------------------------
@Composable
fun ScalesManagementDialog(
    state: GpaUiState,
    onDismiss: () -> Unit,
    onSelectActive: (Long) -> Unit,
    onDeleteScale: (Long) -> Unit,
    onAddScaleClick: () -> Unit,
    onEditScaleClick: (GradeScaleWithEntries) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("GPA Schemes", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
            ) {
                Text(
                    "Switch schemas. Newly entered Course GPA grades will automatically convert based on points of the active schema.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.allScales, key = { it.scale.id }) { scaleWithEntries ->
                        val scale = scaleWithEntries.scale
                        val entriesSummary = scaleWithEntries.entries
                            .sortedByDescending { it.points }
                            .joinToString(", ") { "${it.grade}: ${it.points}" }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (scale.isActive) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                }
                            ),
                            border = if (scale.isActive) {
                                androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectActive(scale.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = scale.isActive,
                                    onClick = { onSelectActive(scale.id) },
                                    modifier = Modifier.testTag("activate_scale_${scale.id}")
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = scale.name,
                                        fontWeight = if (scale.isActive) FontWeight.Bold else FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (entriesSummary.isEmpty()) "No grades mapped" else entriesSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (!scale.isSystem) {
                                    IconButton(onClick = { onEditScaleClick(scaleWithEntries) }) {
                                        Icon(Icons.Filled.Edit, "Edit custom scale")
                                    }
                                    IconButton(
                                        onClick = { onDeleteScale(scale.id) },
                                        modifier = Modifier.testTag("delete_scale_${scale.id}")
                                    ) {
                                        Icon(Icons.Filled.Delete, "Delete Scale", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        confirmButton = {
            Button(
                onClick = onAddScaleClick,
                modifier = Modifier.testTag("create_custom_scale_button")
            ) {
                Icon(Icons.Filled.Add, "Add Scale", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create Scale")
            }
        }
    )
}

// -------------------------------------------------------------
// Interactive creation/edit dynamic scale dialog
// -------------------------------------------------------------
@Composable
fun CreateEditScaleDialog(
    isEdit: Boolean,
    scaleId: Long? = null,
    initialName: String = "",
    initialMaxPoints: Double = 4.0,
    initialEntries: List<GradeEntryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, Double, List<GradeEntryEntity>) -> Unit = { _, _, _ -> },
    onConfirmWithId: (Long?, String, Double, List<GradeEntryEntity>) -> Unit = { _, _, _, _ -> }
) {
    var name by remember { mutableStateOf(initialName) }
    var maxPointsStr by remember { mutableStateOf(initialMaxPoints.toString()) }
    val entryList = remember {
        val list = mutableStateListOf<GradeEntryModel>()
        if (initialEntries.isNotEmpty()) {
            list.addAll(initialEntries.map { GradeEntryModel(it.grade, it.points.toString()) })
        } else {
            // Populate standard templates for fast entry
            list.addAll(
                listOf(
                    GradeEntryModel("A+", "4.0"),
                    GradeEntryModel("A", "4.0"),
                    GradeEntryModel("B", "3.0"),
                    GradeEntryModel("C", "2.0"),
                    GradeEntryModel("F", "0.0")
                )
            )
        }
        list
    }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Grade Scheme" else "New Grade Scheme", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 410.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Scheme Name") },
                    placeholder = { Text("e.g. My University 4.3 Scale") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_scale_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = maxPointsStr,
                    onValueChange = { maxPointsStr = it },
                    label = { Text("Max Points Cap") },
                    placeholder = { Text("e.g. 4.0") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("custom_scale_max_points_input")
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Grade Letter Points Maps:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = { entryList.add(GradeEntryModel("", "")) },
                        modifier = Modifier.height(32.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Add, "Add Grade mapping Entry")
                        Text("Add Map", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(entryList.size) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = entryList[index].grade,
                                onValueChange = { entryList[index] = entryList[index].copy(grade = it.uppercase()) },
                                placeholder = { Text("e.g. A+") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = entryList[index].points,
                                onValueChange = { entryList[index] = entryList[index].copy(points = it) },
                                placeholder = { Text("e.g. 4.0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { entryList.removeAt(index) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.Close, "Delete Map Row", tint = Color.Red)
                            }
                        }
                    }
                }

                errorMsg?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Scheme name is mandatory."
                        return@Button
                    }
                    val maxPoints = maxPointsStr.toDoubleOrNull()
                    if (maxPoints == null || maxPoints <= 0.0) {
                        errorMsg = "Max cap points must be a positive decimal."
                        return@Button
                    }
                    if (entryList.isEmpty()) {
                        errorMsg = "Scheme must map at least one grade letters criteria."
                        return@Button
                    }

                    val entries = mutableListOf<GradeEntryEntity>()
                    for (entry in entryList) {
                        if (entry.grade.isBlank()) {
                            errorMsg = "Map entry grade letter cannot be empty."
                            return@Button
                        }
                        val pts = entry.points.toDoubleOrNull()
                        if (pts == null || pts < 0.0 || pts > maxPoints) {
                            errorMsg = "Points for ${entry.grade} must be a valid number between 0.0 and $maxPoints."
                            return@Button
                        }
                        entries.add(
                            GradeEntryEntity(
                                scaleId = scaleId ?: 0,
                                grade = entry.grade.uppercase().trim(),
                                points = pts
                            )
                        )
                    }

                    // Hand Off to respective listeners
                    if (isEdit) {
                        onConfirmWithId(scaleId, name, maxPoints, entries)
                    } else {
                        onConfirm(name, maxPoints, entries)
                    }
                },
                modifier = Modifier.testTag("custom_scale_confirm_save")
            ) {
                Text("Compile Saving")
            }
        }
    )
}

data class GradeEntryModel(
    val grade: String,
    val points: String
)

// -------------------------------------------------------------
// General Settings / Logic computation dialog
// -------------------------------------------------------------
@Composable
fun AppSettingsDialog(
    cgpaMethod: String,
    onDismiss: () -> Unit,
    onMethodChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Calculation Rules & Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "You can customize how the overall Cumulative CGPA is synthesized from courses and semesters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("CGPA Synthesis Formula Mode:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMethodChange("credits") }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = cgpaMethod == "credits",
                                onClick = { onMethodChange("credits") },
                                modifier = Modifier.testTag("toggle_credits_rule")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Total-Course-Weighted (Standard Pooling)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Evaluates CGPA by pooling all registered classes globally. Heavy-credit courses weigh proportionally more across the entire degree program.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onMethodChange("semester_weights") }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = cgpaMethod == "semester_weights",
                                onClick = { onMethodChange("semester_weights") },
                                modifier = Modifier.testTag("toggle_semester_weights_rule")
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Semester-Weighted Mean Average", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Each semester's total GPA operates as a single grade unit. These units are averaged together based on customizable multipliers (Weights) set under each Semester details card.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("close_settings_button")) {
                Text("Confirm")
            }
        }
    )
}
