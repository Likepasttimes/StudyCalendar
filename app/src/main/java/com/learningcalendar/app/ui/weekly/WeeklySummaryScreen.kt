package com.learningcalendar.app.ui.weekly

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.WeekRange
import com.learningcalendar.app.domain.WeekSummaryDocument
import com.learningcalendar.app.ui.Amber
import com.learningcalendar.app.ui.CardOutline
import com.learningcalendar.app.ui.DeepBlue
import com.learningcalendar.app.ui.MutedBlue
import com.learningcalendar.app.ui.SageDark
import com.learningcalendar.app.ui.SageSoft
import com.learningcalendar.app.ui.SurfaceWarm
import com.learningcalendar.app.ui.WarmBackground
import com.learningcalendar.app.ui.components.openLocalFile
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val weekDateFormatter = DateTimeFormatter.ofPattern("M 月 d 日", Locale.SIMPLIFIED_CHINESE)
private val weekTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.SIMPLIFIED_CHINESE)
private val weekWeekdayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.SIMPLIFIED_CHINESE)

@Composable
fun WeeklySummaryScreen(
    state: WeeklySummaryOverviewUiState,
    bottomBar: @Composable () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenDetail: (LocalDate) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WarmBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "title") {
                Text(
                    text = "周总结",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepBlue,
                )
            }
            item(key = "week-card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    border = BorderStroke(1.dp, CardOutline),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = onPreviousWeek,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("‹") }
                            Text(
                                text = weekRangeLabel(state.weekRange),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepBlue,
                            )
                            TextButton(
                                onClick = onNextWeek,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text("›") }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "本周学习周报",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepBlue,
                                )
                                Text(
                                    text = "查阅任务卡片并上传总结文档",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedBlue,
                                )
                            }
                            FilledTonalButton(
                                onClick = { onOpenDetail(state.weekRange.startDate) },
                                modifier = Modifier.heightIn(min = 48.dp),
                                shape = RoundedCornerShape(15.dp),
                            ) { Text("详情") }
                        }
                    }
                }
            }
            item(key = "monthly-reports-title") {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "本月上传的周总结报告",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepBlue,
                    )
                    Text(
                        text = "${state.displayedMonth.monthValue} 月 · " +
                            "${state.monthlyDocuments.size} 份",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedBlue,
                    )
                }
            }
            if (state.monthlyDocuments.isEmpty() && !state.isLoading) {
                item(key = "monthly-reports-empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("本月还没有上传周总结报告", color = MutedBlue)
                    }
                }
            } else {
                state.monthlyDocuments.forEach { document ->
                    item(key = "monthly-${document.id}") {
                        MonthlySummaryReportCard(
                            document = document,
                            onOpen = {
                                if (!openLocalFile(context, document.localPath, document.mimeType)) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("没有可打开此文档的应用")
                                    }
                                }
                            },
                            onOpenWeek = { onOpenDetail(document.weekStartDate) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryReportCard(
    document: WeekSummaryDocument,
    onOpen: () -> Unit,
    onOpenWeek: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, CardOutline),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = document.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = DeepBlue,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = weekRangeLabel(WeekRange.containing(document.weekStartDate)),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedBlue,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onOpen,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("打开报告") }
                OutlinedButton(
                    onClick = onOpenWeek,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("查看该周") }
            }
        }
    }
}

@Composable
fun WeekSummaryDetailScreen(
    state: WeekSummaryDetailUiState,
    isSaving: Boolean,
    feedback: kotlinx.coroutines.flow.Flow<com.learningcalendar.app.ui.UiFeedback>,
    onBack: () -> Unit,
    onOpenTask: (String) -> Unit,
    onImportDocument: (android.net.Uri, (Boolean) -> Unit) -> Unit,
    onDeleteDocument: (String, (Boolean) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val documentPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImportDocument(uri) {}
    }
    LaunchedEffect(feedback) {
        feedback.collect { snackbarHostState.showSnackbar(it.text) }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),
                    ) { Text("← 返回周总结") }
                    Text(
                        text = "周总结详情",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepBlue,
                    )
                    Text(
                        text = weekRangeLabel(state.weekRange),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedBlue,
                    )
                }
            }
            item(key = "upload") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SageSoft),
                    border = BorderStroke(1.dp, CardOutline),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "周总结文档",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepBlue,
                                )
                                Text(
                                    text = "支持 docx 和 pdf",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedBlue,
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    documentPicker.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        ),
                                    )
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                                enabled = !isSaving,
                            ) { Text("上传周总结") }
                        }
                    }
                }
            }
            item(key = "tasks-title") {
                Text(
                    text = "本周任务卡片",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepBlue,
                )
            }
            if (state.tasks.isEmpty() && !state.isLoading) {
                item(key = "no-task") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("本周没有任务卡片", color = MutedBlue)
                    }
                }
            } else {
                state.tasks.forEach { task ->
                    item(key = task.id) {
                        WeekTaskCard(task = task, onClick = { onOpenTask(task.id) })
                    }
                }
            }
            item(key = "monthly-reports-title") {
                Text(
                    text = "本月上传的周总结报告",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepBlue,
                )
            }
            if (state.monthlyDocuments.isEmpty() && !state.isLoading) {
                item(key = "no-monthly-report") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 88.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("本月还没有上传周总结报告", color = MutedBlue)
                    }
                }
            } else {
                state.monthlyDocuments.forEach { document ->
                    item(key = "monthly-${document.id}") {
                        MonthlySummaryReportRow(
                            document = document,
                            enabled = !isSaving,
                            onOpen = {
                                if (!openLocalFile(context, document.localPath, document.mimeType)) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("没有可打开此文档的应用")
                                    }
                                }
                            },
                            onDelete = { onDeleteDocument(document.id) {} },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryReportRow(
    document: WeekSummaryDocument,
    enabled: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onOpen),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, CardOutline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepBlue,
                )
                Text(
                    text = "${weekRangeLabel(WeekRange.containing(document.weekStartDate))} · " +
                        "上传于 ${uploadDateFormatter.format(
                            Instant.ofEpochMilli(document.uploadedAtEpochMillis)
                                .atZone(ZoneId.systemDefault()),
                        )}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedBlue,
                )
            }
            TextButton(onClick = onDelete, enabled = enabled) {
                Text("删除", color = Amber)
            }
        }
    }
}

@Composable
private fun WeekTaskCard(
    task: StudyTask,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, CardOutline),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${task.scheduledDate.monthValue} 月 ${task.scheduledDate.dayOfMonth} 日 " +
                        task.scheduledDate.format(weekWeekdayFormatter),
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedBlue,
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepBlue,
                )
                Text(
                    text = listOfNotNull(
                        task.startTime?.format(weekTimeFormatter),
                        if (task.section == TaskSection.NEW) "新任务卡片" else "复习卡片",
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedBlue,
                )
            }
            Text(
                text = if (task.isCompleted) "已完成" else "未完成",
                color = if (task.isCompleted) SageDark else Amber,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun weekRangeLabel(range: WeekRange): String =
    "${range.startDate.format(weekDateFormatter)} – ${range.endDate.format(weekDateFormatter)}"

private val uploadDateFormatter =
    DateTimeFormatter.ofPattern("M 月 d 日 HH:mm", Locale.SIMPLIFIED_CHINESE)
