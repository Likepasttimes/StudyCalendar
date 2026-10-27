package com.learningcalendar.app.ui.overview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.learningcalendar.app.domain.CompletionSummary
import com.learningcalendar.app.domain.DailyOverviewLogic
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.ui.Amber
import com.learningcalendar.app.ui.AmberSoft
import com.learningcalendar.app.ui.CardOutline
import com.learningcalendar.app.ui.DeepBlue
import com.learningcalendar.app.ui.MutedBlue
import com.learningcalendar.app.ui.Sage
import com.learningcalendar.app.ui.SageDark
import com.learningcalendar.app.ui.SageSoft
import com.learningcalendar.app.ui.SurfaceWarm
import com.learningcalendar.app.ui.WarmBackground
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.floor

private val overviewDateFormatter = DateTimeFormatter.ofPattern(
    "yyyy 年 M 月 d 日 EEEE",
    Locale.SIMPLIFIED_CHINESE,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyOverviewScreen(
    state: DailyOverviewUiState,
    bottomBar: @Composable () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTaskId by remember { mutableStateOf<String?>(null) }
    val allTasks = state.allTasks

    LaunchedEffect(allTasks.map { it.task.id }) {
        if (selectedTaskId !in allTasks.map { it.task.id }) {
            selectedTaskId = null
        }
    }

    Scaffold(
        containerColor = WarmBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = bottomBar,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "任务完成概况",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue,
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "按天查看新建与复习任务的整体完成情况",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedBlue,
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    border = BorderStroke(1.dp, CardOutline),
                ) {
                    Column(
                        modifier = Modifier.padding(15.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onPreviousDay,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("前一天") }
                            OutlinedButton(
                                onClick = onNextDay,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("后一天") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.selectedDate.format(overviewDateFormatter),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = DeepBlue,
                                )
                                Text(
                                    text = "当天共 ${allTasks.size} 个任务卡",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedBlue,
                                )
                            }
                            TextButton(
                                onClick = { showDatePicker = true },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("选择日期") }
                        }
                        Button(
                            onClick = onToday,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(15.dp),
                        ) { Text("回到今天") }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Sage)
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CompletionRateCard(
                            title = "新建完成率",
                            summary = DailyOverviewLogic.summary(
                                state.newTasks.map { it.task },
                            ),
                            accent = SageDark,
                            container = SageSoft,
                            modifier = Modifier.weight(1f),
                        )
                        CompletionRateCard(
                            title = "复习完成率",
                            summary = DailyOverviewLogic.summary(
                                state.reviewTasks.map { it.task },
                            ),
                            accent = Amber,
                            container = AmberSoft,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                if (allTasks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                            border = BorderStroke(1.dp, CardOutline),
                        ) {
                            Column(modifier = Modifier.padding(22.dp)) {
                                Text(
                                    text = "当天没有任务卡片",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepBlue,
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "切换日期可以查看其他一天的学习完成情况。",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedBlue,
                                )
                            }
                        }
                    }
                } else {
                    item {
                        TaskOverviewChartCard(
                            tasks = allTasks,
                            selectedTaskId = selectedTaskId,
                            onSelectTask = { selectedTaskId = it },
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onSelectDate(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate(),
                            )
                        }
                        showDatePicker = false
                    },
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CompletionRateCard(
    title: String,
    summary: CompletionSummary,
    accent: Color,
    container: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = DeepBlue,
            )
            Text(
                text = if (summary.total == 0) "暂无" else "${summary.rate}%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Text(
                text = if (summary.total == 0) {
                    "0 个任务卡"
                } else {
                    "已完成 ${summary.completed} / ${summary.total}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MutedBlue,
            )
        }
    }
}

@Composable
private fun TaskOverviewChartCard(
    tasks: List<OverviewTaskItem>,
    selectedTaskId: String?,
    onSelectTask: (String) -> Unit,
) {
    val selected = tasks.firstOrNull { it.task.id == selectedTaskId }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, CardOutline),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "当天任务饼图",
                style = MaterialTheme.typography.titleLarge,
                color = DeepBlue,
            )
            Text(
                text = "每块扇形代表一个任务卡；点击扇形或下方任务卡查看内容卡片数量。",
                style = MaterialTheme.typography.bodySmall,
                color = MutedBlue,
            )
            TaskPieChart(
                tasks = tasks,
                selectedTaskId = selectedTaskId,
                onSelectTask = onSelectTask,
            )
            if (selected != null) {
                SelectedTaskSummary(selected)
            }
            tasks.forEach { item ->
                TaskLegendRow(
                    item = item,
                    selected = item.task.id == selectedTaskId,
                    onClick = { onSelectTask(item.task.id) },
                )
            }
        }
    }
}

@Composable
private fun TaskPieChart(
    tasks: List<OverviewTaskItem>,
    selectedTaskId: String?,
    onSelectTask: (String) -> Unit,
) {
    var chartSize by remember { mutableStateOf(IntSize.Zero) }
    val segmentSweep = 360f / tasks.size.coerceAtLeast(1)
    val selectedIndex = tasks.indexOfFirst { it.task.id == selectedTaskId }
    val selectedTitles = tasks.joinToString("；") {
        "${it.task.title}，${if (it.task.isCompleted) "已完成" else "未完成"}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(226.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(210.dp)
                .onSizeChanged { chartSize = it }
                .semantics {
                    contentDescription = "当天任务饼图，共 ${tasks.size} 个任务：$selectedTitles"
                }
                .pointerInput(tasks, chartSize) {
                    detectTapGestures { offset ->
                        if (chartSize.width == 0 || chartSize.height == 0) return@detectTapGestures
                        val centerX = chartSize.width / 2f
                        val centerY = chartSize.height / 2f
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        if (dx * dx + dy * dy > (chartSize.width / 2f).let { it * it }) {
                            return@detectTapGestures
                        }
                        val rawAngle = atan2(dy, dx) * 180f / PI.toFloat()
                        val normalized = (rawAngle + 90f + 360f) % 360f
                        val index = floor(normalized / segmentSweep).toInt()
                            .coerceIn(0, tasks.lastIndex)
                        onSelectTask(tasks[index].task.id)
                    }
                },
        ) {
            tasks.forEachIndexed { index, item ->
                val color = taskColor(item, index)
                drawArc(
                    color = color,
                    startAngle = index * segmentSweep - 90f,
                    sweepAngle = (segmentSweep - 1.2f).coerceAtLeast(0.8f),
                    useCenter = true,
                )
                if (index == selectedIndex) {
                    drawArc(
                        color = SurfaceWarm,
                        startAngle = index * segmentSweep - 90f,
                        sweepAngle = segmentSweep - 1.2f,
                        useCenter = true,
                        style = Stroke(width = 4.dp.toPx()),
                    )
                }
            }
            drawCircle(color = SurfaceWarm, radius = size.minDimension * 0.19f)
            drawCircle(
                color = CardOutline,
                radius = size.minDimension * 0.19f,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = tasks.size.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = DeepBlue,
            )
            Text("任务卡", style = MaterialTheme.typography.bodySmall, color = MutedBlue)
        }
    }
}

@Composable
private fun SelectedTaskSummary(item: OverviewTaskItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SageSoft),
    ) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.task.title,
                style = MaterialTheme.typography.titleMedium,
                color = DeepBlue,
            )
            Text(
                text = if (item.task.section == TaskSection.REVIEW) "复习专区" else "新任务专区",
                style = MaterialTheme.typography.bodySmall,
                color = MutedBlue,
            )
            Text(
                text = if (item.task.isCompleted) "已完成" else "未完成",
                style = MaterialTheme.typography.bodyMedium,
                color = if (item.task.isCompleted) SageDark else Amber,
            )
            Text(
                text = "内容卡片：${item.contentCardCount} 张",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = DeepBlue,
            )
        }
    }
}

@Composable
private fun TaskLegendRow(
    item: OverviewTaskItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${item.task.title}，" +
                    if (item.task.isCompleted) "已完成" else "未完成"
            },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) SageSoft else WarmBackground,
        ),
        border = BorderStroke(1.dp, if (selected) Sage else CardOutline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(
                    color = if (item.task.section == TaskSection.REVIEW) {
                        if (item.task.isCompleted) Amber else AmberSoft
                    } else {
                        if (item.task.isCompleted) SageDark else Sage
                    },
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 9.dp),
            ) {
                Text(
                    text = item.task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DeepBlue,
                )
                Text(
                    text = "${if (item.task.section == TaskSection.REVIEW) "复习" else "新建"} · " +
                        "${if (item.task.isCompleted) "已完成" else "未完成"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedBlue,
                )
            }
            Text(
                text = "${item.contentCardCount} 张",
                style = MaterialTheme.typography.bodySmall,
                color = MutedBlue,
            )
        }
    }
}

private fun taskColor(item: OverviewTaskItem, index: Int): Color {
    val base = when (item.task.section) {
        TaskSection.REVIEW -> if (item.task.isCompleted) Amber else AmberSoft
        TaskSection.NEW -> if (item.task.isCompleted) SageDark else Sage
    }
    val alternate = if (index % 2 == 0) 1f else 0.78f
    return base.copy(alpha = alternate)
}
