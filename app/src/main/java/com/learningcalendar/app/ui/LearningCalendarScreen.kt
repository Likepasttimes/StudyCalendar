package com.learningcalendar.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.TaskSourceType
import com.learningcalendar.app.ui.components.TaskEditorSheet
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val calendarTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.SIMPLIFIED_CHINESE)
private val calendarWeekdays = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

@Composable
fun LearningCalendarScreen(
    state: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenTask: (String) -> Unit,
    isSaving: Boolean,
    feedback: kotlinx.coroutines.flow.Flow<UiFeedback>,
    onCreateTodayTask: (String, LocalTime?, (Boolean) -> Unit) -> Unit,
    onTaskCompleted: (StudyTask, Boolean, (Boolean) -> Unit) -> Unit,
    onEnableRepetition: (String, (Boolean) -> Unit) -> Unit,
    onCancelRepetition: (String, (Boolean) -> Unit) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var calendarExpanded by rememberSaveable { mutableStateOf(true) }
    var showQuickCreate by rememberSaveable { mutableStateOf(false) }
    var pendingUncomplete by remember { mutableStateOf<StudyTask?>(null) }
    var pendingCancelReview by remember { mutableStateOf<StudyTask?>(null) }

    LaunchedEffect(feedback) {
        feedback.collect { snackbarHostState.showSnackbar(it.text) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WarmBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = bottomBar,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickCreate = true },
                modifier = Modifier
                    .size(58.dp)
                    .clearAndSetSemantics {
                        contentDescription = "新建今天的任务卡片"
                        role = Role.Button
                        onClick(label = "新建今天的任务卡片") {
                            showQuickCreate = true
                            true
                        }
                    },
                containerColor = DeepBlue,
                contentColor = Color.White,
                shape = CircleShape,
            ) {
                Text("+", style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 8.dp,
                top = 10.dp,
                end = 8.dp,
                bottom = 88.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                AppHeader(
                    onTodayClick = onTodayClick,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            item(key = "calendar") {
                MonthCalendarCard(
                    state = state,
                    onDateSelected = onDateSelected,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    expanded = calendarExpanded,
                    onToggleExpanded = { calendarExpanded = !calendarExpanded },
                )
            }
            item(key = "selected-day-tasks") {
                SelectedDayTasksCard(
                    state = state,
                    onOpenDay = onOpenDay,
                    onOpenTask = onOpenTask,
                    isSaving = isSaving,
                    onTaskCompleted = { task, completed ->
                        if (!completed && task.spacedRepetitionEnabled) {
                            pendingUncomplete = task
                        } else {
                            onTaskCompleted(task, completed) {}
                        }
                    },
                    onToggleRepetition = { task ->
                        if (task.spacedRepetitionEnabled) {
                            pendingCancelReview = task
                        } else {
                            onEnableRepetition(task.id) {}
                        }
                    },
                )
            }
        }
    }

    if (showQuickCreate) {
        TaskEditorSheet(
            task = null,
            section = TaskSection.NEW,
            isSaving = isSaving,
            onDismiss = { if (!isSaving) showQuickCreate = false },
            onSave = { title, time, onResult ->
                onCreateTodayTask(title, time) { success ->
                    onResult(success)
                    if (success) showQuickCreate = false
                }
            },
        )
    }

    pendingUncomplete?.let { task ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) pendingUncomplete = null },
            title = { Text("取消完成？") },
            text = { Text("取消完成将同时移除二三七复习安排。") },
            confirmButton = {
                Button(
                    onClick = {
                        onTaskCompleted(task, false) { success ->
                            if (success) pendingUncomplete = null
                        }
                    },
                    enabled = !isSaving,
                ) { Text("取消完成并移除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUncomplete = null }, enabled = !isSaving) {
                    Text("返回")
                }
            },
            containerColor = SurfaceWarm,
        )
    }

    pendingCancelReview?.let { task ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) pendingCancelReview = null },
            title = { Text("关闭间隔复习？") },
            text = { Text("将移除第 2、3、7 天复习安排。") },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelRepetition(task.id) { success ->
                            if (success) pendingCancelReview = null
                        }
                    },
                    enabled = !isSaving,
                ) { Text("移除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancelReview = null }, enabled = !isSaving) {
                    Text("保留")
                }
            },
            containerColor = SurfaceWarm,
        )
    }
}

@Composable
private fun AppHeader(
    onTodayClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "学习日历",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = DeepBlue,
        )
        FilledTonalButton(
            onClick = onTodayClick,
            modifier = Modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(15.dp),
            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 9.dp),
        ) {
            Text("回到今天", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun MonthCalendarCard(
    state: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, CardOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 2.dp, vertical = 6.dp)
                .animateContentSize(animationSpec = tween(180)),
        ) {
            if (expanded) {
                MonthNavigation(
                    displayedMonth = state.displayedMonth,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        color = CardOutline,
                    )
                    WeekdayHeader()
                    MonthGrid(
                        state = state,
                        onDateSelected = onDateSelected,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                    )
                }
                TextButton(
                    onClick = onToggleExpanded,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 48.dp),
                ) {
                    Text("收起日历")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${state.displayedMonth.year} 年 " +
                            "${state.displayedMonth.monthValue} 月",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepBlue,
                    )
                    TextButton(
                        onClick = onToggleExpanded,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("展开日历")
                    }
                }
                CompactWeekRow(
                    state = state,
                    onDateSelected = onDateSelected,
                )
            }
        }
    }
}

@Composable
private fun CompactWeekRow(
    state: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
) {
    val week = state.weeks.firstOrNull { week ->
        week.any { it.date == state.selectedDate }
    }.orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        week.forEach { day ->
            val shape = RoundedCornerShape(13.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 1.dp, vertical = 1.dp)
                    .clip(shape)
                    .background(if (day.isSelected) SageSoft else Color.Transparent)
                    .then(
                        if (day.isToday) Modifier.border(1.dp, Amber, shape) else Modifier,
                    )
                    .clickable { onDateSelected(day.date) }
                    .clearAndSetSemantics {
                        contentDescription = calendarDaySemantics(day)
                        selected = day.isSelected
                        role = Role.Button
                        onClick(label = "选择${day.date.dayOfMonth}日") {
                            onDateSelected(day.date)
                            true
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = calendarWeekdays[day.date.dayOfWeek.value - 1].removePrefix("星期"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedBlue,
                    )
                    Text(
                        text = day.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            day.isSelected -> SageDark
                            !day.isInDisplayedMonth -> MutedBlue.copy(alpha = 0.72f)
                            else -> DeepBlue
                        },
                        fontWeight = if (day.isSelected || day.isToday) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .background(
                                color = if (day.taskCount > 0) Sage else Color.Transparent,
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDayTasksCard(
    state: CalendarUiState,
    onOpenDay: (LocalDate) -> Unit,
    onOpenTask: (String) -> Unit,
    isSaving: Boolean,
    onTaskCompleted: (StudyTask, Boolean) -> Unit,
    onToggleRepetition: (StudyTask) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, CardOutline),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = selectedDateLabel(state.selectedDate),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepBlue,
                )
                TextButton(
                    onClick = { onOpenDay(state.selectedDate) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
                ) {
                    Text("查看当天卡片")
                }
            }

            if (state.selectedTasks.isEmpty()) {
                Text(
                    text = "当天没有任务卡片",
                    modifier = Modifier.padding(vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedBlue,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.selectedTasks.forEach { task ->
                        CalendarTaskRow(
                            task = task,
                            isSaving = isSaving,
                            onClick = { onOpenTask(task.id) },
                            onCompleted = onTaskCompleted,
                            onToggleRepetition = { onToggleRepetition(task) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTaskRow(
    task: StudyTask,
    isSaving: Boolean,
    onClick: () -> Unit,
    onCompleted: (StudyTask, Boolean) -> Unit,
    onToggleRepetition: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ScheduleSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onClick)
                    .clearAndSetSemantics {
                        val timeLabel = task.startTime?.format(calendarTimeFormatter)
                            ?.let { "，$it" }
                            .orEmpty()
                        contentDescription = "查看任务卡片，${task.title}$timeLabel，" +
                            if (task.isCompleted) "已完成" else "未完成"
                        role = Role.Button
                        onClick(label = "查看${task.title}") {
                            onClick()
                            true
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (task.startTime != null) {
                    Text(
                        text = task.startTime.format(calendarTimeFormatter),
                        style = MaterialTheme.typography.labelLarge,
                        color = SageDark,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DeepBlue,
                    )
                    Text(
                        text = if (task.section == TaskSection.NEW) "新任务卡片" else "复习卡片",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedBlue,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (task.isCompleted) SageSoft else AmberSoft,
                ) {
                    Text(
                        text = if (task.isCompleted) "已完成" else "未完成",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (task.isCompleted) SageDark else Amber,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                    .weight(if (task.canOfferSpacedRepetition) 1f else 2f)
                    .heightIn(min = 48.dp)
                    .clickable(enabled = !isSaving) {
                        onCompleted(task, !task.isCompleted)
                    }
                    .clearAndSetSemantics {
                        contentDescription = if (task.isCompleted) {
                            "卡片已完成，点击取消完成"
                        } else {
                            "卡片未完成，点击标记完成"
                        }
                        role = Role.Checkbox
                        onClick(label = "切换卡片完成") {
                            onCompleted(task, !task.isCompleted)
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
                    color = DeepBlue,
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
        }
    }
}

private fun selectedDateLabel(date: LocalDate): String =
    "${date.monthValue} 月 ${date.dayOfMonth} 日 ${calendarWeekdays[date.dayOfWeek.value - 1]}"

@Composable
private fun MonthNavigation(
    displayedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthArrowButton(
            pointsLeft = true,
            contentDescription = "上一月",
            onClick = onPreviousMonth,
        )
        AnimatedContent(
            targetState = displayedMonth,
            modifier = Modifier.weight(1f),
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
            label = "月份标题",
        ) { month ->
            Text(
                text = "${month.year} 年 ${month.monthValue} 月",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = DeepBlue,
                textAlign = TextAlign.Center,
            )
        }
        MonthArrowButton(
            pointsLeft = false,
            contentDescription = "下一月",
            onClick = onNextMonth,
        )
    }
}

@Composable
private fun MonthArrowButton(
    pointsLeft: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier
            .size(48.dp)
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                role = Role.Button
                onClick(label = contentDescription) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                    true
                }
            },
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            val strokeWidth = 2.dp.toPx()
            val startX = if (pointsLeft) size.width * 0.75f else size.width * 0.25f
            val tipX = if (pointsLeft) size.width * 0.30f else size.width * 0.70f
            drawLine(
                color = DeepBlue,
                start = Offset(startX, size.height * 0.20f),
                end = Offset(tipX, size.height * 0.50f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = DeepBlue,
                start = Offset(tipX, size.height * 0.50f),
                end = Offset(startX, size.height * 0.80f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
    ) {
        weekdays.forEachIndexed { index, weekday ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 30.dp)
                    .clearAndSetSemantics { contentDescription = "星期$weekday" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = weekday,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (index >= 5) SageDark else MutedBlue,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private data class MonthGridPresentation(
    val month: YearMonth,
    val weeks: List<List<CalendarDayUi>>,
)

@Composable
private fun MonthGrid(
    state: CalendarUiState,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val threshold = with(density) { 48.dp.toPx() }
    val dragLimit = with(density) { 18.dp.toPx() }
    var horizontalDrag by remember(state.displayedMonth) { mutableFloatStateOf(0f) }
    val translatedDrag by animateFloatAsState(
        targetValue = horizontalDrag.coerceIn(-dragLimit, dragLimit),
        animationSpec = tween(80),
        label = "滑动反馈",
    )

    AnimatedContent(
        targetState = MonthGridPresentation(state.displayedMonth, state.weeks),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = translatedDrag }
            .pointerInput(state.displayedMonth, threshold) {
                var dragDistance = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragDistance = 0f
                        horizontalDrag = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragDistance += dragAmount
                        horizontalDrag = dragDistance
                    },
                    onDragCancel = { horizontalDrag = 0f },
                    onDragEnd = {
                        horizontalDrag = 0f
                        if (abs(dragDistance) >= threshold) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (dragDistance < 0f) onNextMonth() else onPreviousMonth()
                        }
                    },
                )
            },
        contentKey = MonthGridPresentation::month,
        transitionSpec = {
            val movingForward = targetState.month > initialState.month
            val enterOffset: (Int) -> Int = { if (movingForward) it / 4 else -it / 4 }
            val exitOffset: (Int) -> Int = { if (movingForward) -it / 4 else it / 4 }
            (slideInHorizontally(tween(190), enterOffset) + fadeIn(tween(150))) togetherWith
                (slideOutHorizontally(tween(170), exitOffset) + fadeOut(tween(110)))
        },
        label = "月历切换",
    ) { presentation ->
        Column(modifier = Modifier.fillMaxWidth()) {
            presentation.weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        CalendarDayCell(
                            day = day,
                            onDateSelected = onDateSelected,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDayUi,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(15.dp)
    val backgroundColor by animateColorAsState(
        targetValue = if (day.isSelected) SageSoft else Color.Transparent,
        animationSpec = tween(150),
        label = "日期选中颜色",
    )
    val numberColor = when {
        day.isSelected -> SageDark
        !day.isInDisplayedMonth -> MutedBlue.copy(alpha = 0.72f)
        else -> DeepBlue
    }
    val semanticsLabel = calendarDaySemantics(day)
    val selectDate = { onDateSelected(day.date) }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .padding(horizontal = 1.dp, vertical = 1.dp)
            .clip(shape)
            .clickable(onClick = selectDate)
            .clearAndSetSemantics {
                contentDescription = semanticsLabel
                selected = day.isSelected
                role = Role.Button
                onClick(label = "选择${day.date.dayOfMonth}日") {
                    selectDate()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor, shape)
                .then(
                    if (day.isToday) Modifier.border(1.5.dp, Amber, shape) else Modifier,
                )
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = numberColor,
                    fontWeight = if (day.isToday || day.isSelected) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            color = if (day.taskCount > 0) {
                                if (day.isSelected) SageDark else Sage
                            } else {
                                Color.Transparent
                            },
                            shape = CircleShape,
                        ),
                )
            }
        }
    }
}

private fun calendarDaySemantics(day: CalendarDayUi): String {
    val dateLabel = "${day.date.year} 年 ${day.date.monthValue} 月 ${day.date.dayOfMonth} 日"
    val labels = buildList {
        add(dateLabel)
        if (day.isToday) add("今天")
        add(if (day.taskCount > 0) "有 ${day.taskCount} 张任务卡片" else "当天没有任务卡片")
    }
    return labels.joinToString("，")
}

@Preview(name = "窄屏", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "大字体", widthDp = 360, heightDp = 900, fontScale = 1.8f, showBackground = true)
@Composable
private fun LearningCalendarPreview() {
    val today = LocalDate.of(2026, 10, 25)
    val tasks = listOf(
        StudyTask(
            id = "preview-review",
            title = "英语单词复习",
            scheduledDate = today,
            startTime = LocalTime.of(8, 30),
            section = TaskSection.REVIEW,
            isCompleted = false,
            spacedRepetitionEnabled = false,
            createdDate = today.minusDays(1),
            createdAtEpochMillis = 1,
            sourceType = TaskSourceType.AUTO_REVIEW,
            sourceTaskId = "preview-source",
            reviewSequence = 2,
        ),
        StudyTask(
            id = "preview-new",
            title = "数学错题整理",
            scheduledDate = today,
            startTime = LocalTime.of(16, 0),
            section = TaskSection.NEW,
            isCompleted = false,
            spacedRepetitionEnabled = false,
            createdDate = today,
            createdAtEpochMillis = 2,
            sourceType = TaskSourceType.ORIGINAL,
            sourceTaskId = null,
            reviewSequence = null,
        ),
    )
    val state = CalendarUiState.create(
        displayedMonth = YearMonth.from(today),
        selectedDate = today,
        today = today,
        taskCounts = mapOf(
            today to 2,
            today.plusDays(1) to 1,
            today.plusDays(6) to 3,
        ),
        selectedTasks = tasks,
    )
    LearningCalendarTheme {
        LearningCalendarScreen(
            state = state,
            onDateSelected = {},
            onOpenDay = {},
            onOpenTask = {},
            isSaving = false,
            feedback = kotlinx.coroutines.flow.emptyFlow(),
            onCreateTodayTask = { _, _, _ -> },
            onTaskCompleted = { _, _, _ -> },
            onEnableRepetition = { _, _ -> },
            onCancelRepetition = { _, _ -> },
            onPreviousMonth = {},
            onNextMonth = {},
            onTodayClick = {},
        )
    }
}
