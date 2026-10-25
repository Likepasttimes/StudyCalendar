package com.learningcalendar.app.ui.day

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.TaskSourceType
import com.learningcalendar.app.ui.Amber
import com.learningcalendar.app.ui.AmberSoft
import com.learningcalendar.app.ui.CardOutline
import com.learningcalendar.app.ui.DeepBlue
import com.learningcalendar.app.ui.LearningCalendarTheme
import com.learningcalendar.app.ui.MutedBlue
import com.learningcalendar.app.ui.Sage
import com.learningcalendar.app.ui.SageDark
import com.learningcalendar.app.ui.SageSoft
import com.learningcalendar.app.ui.ScheduleSurface
import com.learningcalendar.app.ui.SurfaceWarm
import com.learningcalendar.app.ui.WarmBackground
import com.learningcalendar.app.ui.components.TaskEditorSheet
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val weekdayNames = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")
private val displayTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.SIMPLIFIED_CHINESE)

@Composable
fun DayTasksScreen(
    state: DayTasksUiState,
    isSaving: Boolean,
    feedback: kotlinx.coroutines.flow.Flow<com.learningcalendar.app.ui.UiFeedback>,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    onCreateTask: (TaskSection, String, LocalTime?, (Boolean) -> Unit) -> Unit,
    onUpdateTask: (String, String, LocalTime?, (Boolean) -> Unit) -> Unit,
    onTaskCompleted: (StudyTask, Boolean, (Boolean) -> Unit) -> Unit,
    onEnableRepetition: (String, (Boolean) -> Unit) -> Unit,
    onCancelRepetition: (String, (Boolean) -> Unit) -> Unit,
    onDeleteTask: (String, (Boolean) -> Unit) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var editorTask by remember { mutableStateOf<StudyTask?>(null) }
    var editorSection by remember { mutableStateOf<TaskSection?>(null) }
    var deleteTarget by remember { mutableStateOf<StudyTask?>(null) }
    var cancelReviewTarget by remember { mutableStateOf<StudyTask?>(null) }
    var uncompleteTarget by remember { mutableStateOf<StudyTask?>(null) }

    LaunchedEffect(feedback) {
        feedback.collect { message ->
            snackbarHostState.showSnackbar(message.text)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WarmBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),
                    ) {
                        Text("← 返回月历", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = fullDateLabel(state.date),
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepBlue,
                    )
                    if (state.date == state.today) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusPill("今天", AmberSoft, Amber)
                        }
                    }
                }
            }

            item(key = "review-section") {
                TaskSectionCard(
                    title = "复习卡片专区",
                    emptyText = "复习卡片专区还没有卡片",
                    section = TaskSection.REVIEW,
                    tasks = state.reviewTasks,
                    isSaving = isSaving,
                    onAdd = {
                        editorTask = null
                        editorSection = TaskSection.REVIEW
                    },
                    onOpenTask = onOpenTask,
                    onEdit = {
                        editorTask = it
                        editorSection = it.section
                    },
                    onDelete = { deleteTarget = it },
                    onCompleted = { task, completed ->
                        if (!completed && task.spacedRepetitionEnabled) {
                            uncompleteTarget = task
                        } else {
                            onTaskCompleted(task, completed) {}
                        }
                    },
                    onToggleRepetition = { task ->
                        if (task.spacedRepetitionEnabled) {
                            cancelReviewTarget = task
                        } else {
                            onEnableRepetition(task.id) {}
                        }
                    },
                )
            }

            item(key = "new-section") {
                TaskSectionCard(
                    title = "新任务卡片专区",
                    emptyText = "新任务卡片专区还没有卡片",
                    section = TaskSection.NEW,
                    tasks = state.newTasks,
                    isSaving = isSaving,
                    onAdd = {
                        editorTask = null
                        editorSection = TaskSection.NEW
                    },
                    onOpenTask = onOpenTask,
                    onEdit = {
                        editorTask = it
                        editorSection = it.section
                    },
                    onDelete = { deleteTarget = it },
                    onCompleted = { task, completed ->
                        if (!completed && task.spacedRepetitionEnabled) {
                            uncompleteTarget = task
                        } else {
                            onTaskCompleted(task, completed) {}
                        }
                    },
                    onToggleRepetition = { task ->
                        if (task.spacedRepetitionEnabled) {
                            cancelReviewTarget = task
                        } else {
                            onEnableRepetition(task.id) {}
                        }
                    },
                )
            }

            item(key = "completed-section") {
                TaskSectionCard(
                    title = "已完成卡片专区",
                    emptyText = "还没有已完成卡片",
                    section = TaskSection.NEW,
                    tasks = state.completedTasks,
                    isSaving = isSaving,
                    showAddButton = false,
                    onAdd = {},
                    onOpenTask = onOpenTask,
                    onEdit = {
                        editorTask = it
                        editorSection = it.section
                    },
                    onDelete = { deleteTarget = it },
                    onCompleted = { task, completed ->
                        if (!completed && task.spacedRepetitionEnabled) {
                            uncompleteTarget = task
                        } else {
                            onTaskCompleted(task, completed) {}
                        }
                    },
                    onToggleRepetition = { task ->
                        if (task.spacedRepetitionEnabled) {
                            cancelReviewTarget = task
                        } else {
                            onEnableRepetition(task.id) {}
                        }
                    },
                )
            }

            if (state.isLoading) {
                item(key = "loading") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = Sage)
                    }
                }
            }
        }
    }

    editorSection?.let { section ->
        TaskEditorSheet(
            task = editorTask,
            section = section,
            isSaving = isSaving,
            onDismiss = {
                if (!isSaving) {
                    editorTask = null
                    editorSection = null
                }
            },
            onSave = { title, time, onResult ->
                val currentTask = editorTask
                if (currentTask == null) {
                    onCreateTask(section, title, time) { success ->
                        onResult(success)
                        if (success) {
                            editorTask = null
                            editorSection = null
                        }
                    }
                } else {
                    onUpdateTask(currentTask.id, title, time) { success ->
                        onResult(success)
                        if (success) {
                            editorTask = null
                            editorSection = null
                        }
                    }
                }
            },
        )
    }

    deleteTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) deleteTarget = null },
            title = { Text("删除卡片？") },
            text = {
                Text(
                    if (task.sourceType == TaskSourceType.ORIGINAL) {
                        "删除原始卡片会同时删除关联内容卡片及自动生成的复习卡片，无法通过返回恢复。"
                    } else {
                        "该卡片会被永久删除，无法通过返回恢复。"
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTask(task.id) { success ->
                            if (success) deleteTarget = null
                        }
                    },
                    enabled = !isSaving,
                ) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }, enabled = !isSaving) {
                    Text("取消")
                }
            },
            containerColor = SurfaceWarm,
        )
    }

    cancelReviewTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) cancelReviewTarget = null },
            title = { Text("关闭间隔复习？") },
            text = { Text("将删除由该卡片生成的第 2、3、7 天复习安排。") },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelRepetition(task.id) { success ->
                            if (success) cancelReviewTarget = null
                        }
                    },
                    enabled = !isSaving,
                ) { Text("移除复习安排") }
            },
            dismissButton = {
                TextButton(onClick = { cancelReviewTarget = null }, enabled = !isSaving) {
                    Text("保留")
                }
            },
            containerColor = SurfaceWarm,
        )
    }

    uncompleteTarget?.let { task ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) uncompleteTarget = null },
            title = { Text("取消卡片完成？") },
            text = { Text("取消完成将同时移除二三七复习安排，卡片会回到原专区。") },
            confirmButton = {
                Button(
                    onClick = {
                        onTaskCompleted(task, false) { success ->
                            if (success) uncompleteTarget = null
                        }
                    },
                    enabled = !isSaving,
                ) { Text("取消完成并移除") }
            },
            dismissButton = {
                TextButton(onClick = { uncompleteTarget = null }, enabled = !isSaving) {
                    Text("返回")
                }
            },
            containerColor = SurfaceWarm,
        )
    }
}

@Composable
private fun TaskSectionCard(
    title: String,
    emptyText: String,
    section: TaskSection,
    tasks: List<StudyTask>,
    isSaving: Boolean,
    showAddButton: Boolean = true,
    onAdd: () -> Unit,
    onOpenTask: (String) -> Unit,
    onEdit: (StudyTask) -> Unit,
    onDelete: (StudyTask) -> Unit,
    onCompleted: (StudyTask, Boolean) -> Unit,
    onToggleRepetition: (StudyTask) -> Unit,
) {
    val accent = if (section == TaskSection.NEW) Sage else Amber
    val tint = if (section == TaskSection.NEW) SageSoft else AmberSoft

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, CardOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                StatusPill(title, tint, accent)
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = "${tasks.size} 张任务卡片",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedBlue,
                )
                if (showAddButton) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(if (section == TaskSection.NEW) "新建卡片" else "新建复习卡片")
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when {
                tasks.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 92.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = emptyText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MutedBlue,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        tasks.forEach { task ->
                            TaskRowCard(
                                task = task,
                                isSaving = isSaving,
                                onOpen = { onOpenTask(task.id) },
                                onEdit = { onEdit(task) },
                                onDelete = { onDelete(task) },
                                onCompleted = { onCompleted(task, it) },
                                onToggleRepetition = { onToggleRepetition(task) },
                            )
                        }
                    }
                    if (tasks.all(StudyTask::isCompleted)) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "本专区卡片已全部完成",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SageDark,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRowCard(
    task: StudyTask,
    isSaving: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCompleted: (Boolean) -> Unit,
    onToggleRepetition: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ScheduleSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .clearAndSetSemantics {
                        contentDescription = "查看卡片详情，${task.title}，" +
                            if (task.section == TaskSection.NEW) "新任务卡片专区" else "复习卡片专区"
                        role = Role.Button
                        onClick(label = "查看${task.title}详情") {
                            onOpen()
                            true
                        }
                    },
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (task.startTime != null) {
                        Text(
                            text = task.startTime.format(displayTimeFormatter),
                            style = MaterialTheme.typography.labelLarge,
                            color = SageDark,
                        )
                    }
                    StatusPill(
                        text = if (task.section == TaskSection.NEW) "新任务卡片" else "复习卡片",
                        background = if (task.section == TaskSection.NEW) SageSoft else AmberSoft,
                        foreground = if (task.section == TaskSection.NEW) SageDark else Amber,
                    )
                }
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepBlue,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "来源：${taskOriginLabel(task)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedBlue,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(if (task.canOfferSpacedRepetition) 1f else 2f)
                        .heightIn(min = 48.dp)
                        .clickable(enabled = !isSaving) { onCompleted(!task.isCompleted) }
                        .clearAndSetSemantics {
                            contentDescription = if (task.isCompleted) {
                                "卡片已完成，点击取消完成"
                            } else {
                                "卡片未完成，点击标记完成"
                            }
                            role = Role.Checkbox
                            onClick(label = if (task.isCompleted) "取消完成" else "标记完成") {
                                onCompleted(!task.isCompleted)
                                true
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = null,
                        enabled = !isSaving,
                    )
                    Text(
                        text = if (task.isCompleted) "已完成" else "标记完成",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (task.isCompleted) SageDark else DeepBlue,
                        fontWeight = if (task.isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }

                if (task.canOfferSpacedRepetition) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable(enabled = !isSaving, onClick = onToggleRepetition)
                            .clearAndSetSemantics {
                                contentDescription = if (task.spacedRepetitionEnabled) {
                                    "已加入间隔复习，点击关闭"
                                } else {
                                    "添加到间隔复习"
                                }
                                role = Role.Checkbox
                                onClick(label = "切换间隔复习") {
                                    onToggleRepetition()
                                    true
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = task.spacedRepetitionEnabled,
                            onCheckedChange = null,
                            enabled = !isSaving,
                        )
                        Text(
                            text = if (task.spacedRepetitionEnabled) {
                                "关闭间隔复习"
                            } else {
                                "添加到间隔复习"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepBlue,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                ) { Text("编辑卡片") }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Amber.copy(alpha = 0.65f)),
                ) { Text("删除卡片", color = Amber) }
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = background,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
        )
    }
}

private fun taskOriginLabel(task: StudyTask): String = when {
    task.sourceType == TaskSourceType.AUTO_REVIEW -> "第 ${task.reviewSequence} 天复习卡片"
    task.sourceType == TaskSourceType.MANUAL_REVIEW -> "手动复习卡片"
    else -> "原始卡片"
}

private fun fullDateLabel(date: LocalDate): String =
    "${date.year} 年 ${date.monthValue} 月 ${date.dayOfMonth} 日 ${weekdayNames[date.dayOfWeek.value - 1]}"

@Preview(name = "当天卡片", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun DayTasksPreview() {
    val today = LocalDate.of(2026, 10, 25)
    val task = StudyTask(
        id = "preview",
        title = "整理分数应用题中的单位一错误",
        scheduledDate = today,
        startTime = LocalTime.of(9, 0),
        section = TaskSection.NEW,
        isCompleted = true,
        spacedRepetitionEnabled = false,
        createdDate = today,
        createdAtEpochMillis = 1,
        sourceType = TaskSourceType.ORIGINAL,
        sourceTaskId = null,
        reviewSequence = null,
    )
    LearningCalendarTheme {
        DayTasksScreen(
            state = DayTasksUiState.create(today, today, listOf(task)),
            isSaving = false,
            feedback = kotlinx.coroutines.flow.emptyFlow(),
            onBack = {},
            onOpenTask = {},
            onCreateTask = { _, _, _, _ -> },
            onUpdateTask = { _, _, _, _ -> },
            onTaskCompleted = { _, _, _ -> },
            onEnableRepetition = { _, _ -> },
            onCancelRepetition = { _, _ -> },
            onDeleteTask = { _, _ -> },
        )
    }
}
