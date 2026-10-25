package com.learningcalendar.app.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.learningcalendar.app.domain.MasteryLevel
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskContentCard
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

private val compactTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.SIMPLIFIED_CHINESE)

@Composable
fun TaskDetailScreen(
    state: TaskDetailUiState,
    isSaving: Boolean,
    feedback: kotlinx.coroutines.flow.Flow<com.learningcalendar.app.ui.UiFeedback>,
    onBack: () -> Unit,
    onOpenAcceptance: (String) -> Unit,
    onUpdateTask: (String, LocalTime?, (Boolean) -> Unit) -> Unit,
    onTaskCompleted: (StudyTask, Boolean, (Boolean) -> Unit) -> Unit,
    onEnableRepetition: ((Boolean) -> Unit) -> Unit,
    onCancelRepetition: ((Boolean) -> Unit) -> Unit,
    onDeleteTask: ((Boolean) -> Unit) -> Unit,
    onAddCard: (String, String, String, (Boolean) -> Unit) -> Unit,
    onUpdateCard: (String, String, String, String, (Boolean) -> Unit) -> Unit,
    onDeleteCard: (String, (Boolean) -> Unit) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showTaskEditor by remember { mutableStateOf(false) }
    var showDeleteTask by remember { mutableStateOf(false) }
    var showCancelReview by remember { mutableStateOf(false) }
    var showUncomplete by remember { mutableStateOf(false) }
    var cardEditorTarget by remember { mutableStateOf<CardEditorTarget?>(null) }
    var cardDeleteTarget by remember { mutableStateOf<TaskContentCard?>(null) }
    val expandedCards = remember { mutableStateMapOf<String, Boolean>() }
    val task = state.task

    LaunchedEffect(feedback) {
        feedback.collect { snackbarHostState.showSnackbar(it.text) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = WarmBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (task != null) {
                Surface(color = WarmBackground) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        Button(
                            onClick = {
                                cardEditorTarget = CardEditorTarget.Add
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp),
                            enabled = !isSaving,
                            shape = RoundedCornerShape(17.dp),
                        ) {
                            Text("添加内容卡片")
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = Sage)
            }
            return@Scaffold
        }
        if (task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("卡片不存在或已被删除", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),
                    ) { Text("← 返回当天卡片") }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepBlue,
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "${task.scheduledDate.year} 年 ${task.scheduledDate.monthValue} 月 " +
                            "${task.scheduledDate.dayOfMonth} 日" +
                            (task.startTime?.format(compactTimeFormatter)?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedBlue,
                    )
                }
            }

            item(key = "task-state") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    border = BorderStroke(1.dp, CardOutline),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SmallBadge(
                                    if (task.isCompleted) "已完成" else "未完成",
                                    SageSoft,
                                    SageDark,
                                )
                                SmallBadge(
                                    when (task.sourceType) {
                                        TaskSourceType.ORIGINAL -> "原始卡片"
                                        TaskSourceType.MANUAL_REVIEW -> "手动复习卡片"
                                        TaskSourceType.AUTO_REVIEW -> "第 ${task.reviewSequence} 天复习卡片"
                                    },
                                    AmberSoft,
                                    Amber,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                .weight(if (task.canOfferSpacedRepetition) 1f else 2f)
                                .heightIn(min = 48.dp)
                                .clickable(enabled = !isSaving) {
                                    if (task.isCompleted && task.spacedRepetitionEnabled) {
                                        showUncomplete = true
                                    } else {
                                        onTaskCompleted(task, !task.isCompleted) {}
                                    }
                                }
                                .clearAndSetSemantics {
                                    contentDescription = if (task.isCompleted) {
                                        "卡片已完成，点击取消完成"
                                    } else {
                                        "卡片未完成，点击标记完成"
                                    }
                                    role = Role.Checkbox
                                    onClick(label = "切换卡片完成") {
                                        if (task.isCompleted && task.spacedRepetitionEnabled) {
                                            showUncomplete = true
                                        } else {
                                            onTaskCompleted(task, !task.isCompleted) {}
                                        }
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
                                text = if (task.isCompleted) "卡片已完成" else "标记卡片完成",
                                style = MaterialTheme.typography.bodyLarge,
                                color = DeepBlue,
                            )
                        }

                        if (task.canOfferSpacedRepetition) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clickable(enabled = !isSaving) {
                                        if (task.spacedRepetitionEnabled) {
                                            showCancelReview = true
                                        } else {
                                            onEnableRepetition {}
                                        }
                                    }
                                    .clearAndSetSemantics {
                                        contentDescription = if (task.spacedRepetitionEnabled) {
                                            "已加入间隔复习，点击关闭"
                                        } else {
                                            "添加到间隔复习"
                                        }
                                        role = Role.Checkbox
                                        onClick(label = "切换间隔复习") {
                                            if (task.spacedRepetitionEnabled) {
                                                showCancelReview = true
                                            } else {
                                                onEnableRepetition {}
                                            }
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
                                    if (task.spacedRepetitionEnabled) {
                                        "关闭间隔复习"
                                    } else {
                                        "添加到间隔复习"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
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
                                onClick = { showTaskEditor = true },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp),
                            ) { Text("编辑卡片") }
                            OutlinedButton(
                                onClick = { showDeleteTask = true },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Amber.copy(alpha = 0.65f)),
                            ) { Text("删除卡片", color = Amber) }
                        }
                    }
                }
            }

            item(key = "card-title") {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(
                        text = "内容卡片",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepBlue,
                    )
                }
            }

            if (state.cards.isEmpty()) {
                item(key = "empty-cards") {
                    EmptyCards()
                }
            } else {
                item(key = "timeline") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        state.cards.forEachIndexed { index, card ->
                            TimelineCard(
                                card = card,
                                stepNumber = index + 1,
                                isFirst = index == 0,
                                isLast = index == state.cards.lastIndex,
                                expanded = expandedCards[card.id] == true,
                                isSaving = isSaving,
                                onToggleExpanded = {
                                    expandedCards[card.id] = expandedCards[card.id] != true
                                },
                                onOpenAcceptance = { onOpenAcceptance(card.id) },
                                onEdit = { cardEditorTarget = CardEditorTarget.Edit(card) },
                                onDelete = { cardDeleteTarget = card },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTaskEditor && task != null) {
        TaskEditorSheet(
            task = task,
            section = task.section,
            isSaving = isSaving,
            onDismiss = { if (!isSaving) showTaskEditor = false },
            onSave = { title, time, result ->
                onUpdateTask(title, time) { success ->
                    result(success)
                    if (success) showTaskEditor = false
                }
            },
        )
    }

    showDeleteTask.takeIf { it && task != null }?.let {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showDeleteTask = false },
            title = { Text("删除卡片？") },
            text = {
                Text(
                    if (task?.sourceType == TaskSourceType.ORIGINAL) {
                        "删除原始卡片会级联删除关联内容卡片和自动复习卡片，无法通过返回恢复。"
                    } else {
                        "该卡片及其内容卡片将被永久删除，无法通过返回恢复。"
                    },
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTask { success ->
                            if (success) {
                                showDeleteTask = false
                                onBack()
                            }
                        }
                    },
                    enabled = !isSaving,
                ) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteTask = false }) { Text("取消") }
            },
            containerColor = SurfaceWarm,
        )
    }

    if (showCancelReview && task != null) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showCancelReview = false },
            title = { Text("关闭间隔复习？") },
            text = { Text("将移除该卡片自动生成的第 2、3、7 天复习安排。") },
            confirmButton = {
                Button(
                    onClick = {
                        onCancelRepetition { success ->
                            if (success) showCancelReview = false
                        }
                    },
                    enabled = !isSaving,
                ) { Text("移除复习安排") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelReview = false }) { Text("保留") }
            },
            containerColor = SurfaceWarm,
        )
    }

    if (showUncomplete && task != null) {
        AlertDialog(
            onDismissRequest = { if (!isSaving) showUncomplete = false },
            title = { Text("取消卡片完成？") },
            text = { Text("取消完成将同时移除二三七复习安排，卡片会回到原专区。") },
            confirmButton = {
                Button(
                    onClick = {
                        onTaskCompleted(task, false) { success ->
                            if (success) showUncomplete = false
                        }
                    },
                    enabled = !isSaving,
                ) { Text("取消完成并移除") }
            },
            dismissButton = {
                TextButton(onClick = { showUncomplete = false }) { Text("返回") }
            },
            containerColor = SurfaceWarm,
        )
    }

    cardEditorTarget?.let { target ->
        CardEditorDialog(
            target = target,
            isSaving = isSaving,
            onDismiss = { if (!isSaving) cardEditorTarget = null },
            onSave = { objective, action, criteria, result ->
                when (target) {
                    CardEditorTarget.Add -> {
                        onAddCard(objective, action, criteria) { success ->
                            result(success)
                            if (success) cardEditorTarget = null
                        }
                    }

                    is CardEditorTarget.Edit -> {
                        onUpdateCard(target.card.id, objective, action, criteria) { success ->
                            result(success)
                            if (success) cardEditorTarget = null
                        }
                    }
                }
            },
        )
    }

    cardDeleteTarget?.let { card ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) cardDeleteTarget = null },
            title = { Text("删除这张内容卡片？") },
            text = { Text("卡片目标、初始动作、验收标准、完成状态和掌握度都会一并删除，无法通过返回恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCard(card.id) { success ->
                            if (success) {
                                expandedCards.remove(card.id)
                                cardDeleteTarget = null
                            }
                        }
                    },
                    enabled = !isSaving,
                ) { Text("确认删除卡片") }
            },
            dismissButton = {
                TextButton(onClick = { cardDeleteTarget = null }) { Text("取消") }
            },
            containerColor = SurfaceWarm,
        )
    }
}

@Composable
private fun EmptyCards() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, CardOutline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "还没有内容卡片",
                style = MaterialTheme.typography.titleMedium,
                color = DeepBlue,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "点击底部按钮添加第一张卡片。",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedBlue,
            )
        }
    }
}

@Composable
private fun TimelineCard(
    card: TaskContentCard,
    stepNumber: Int,
    isFirst: Boolean,
    isLast: Boolean,
    expanded: Boolean,
    isSaving: Boolean,
    onToggleExpanded: () -> Unit,
    onOpenAcceptance: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
    ) {
        Canvas(
            modifier = Modifier
                .width(30.dp)
                .fillMaxHeight(),
        ) {
            val centerX = size.width / 2f
            val dotY = 28.dp.toPx()
            val lineColor = CardOutline
            if (!isFirst) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, 0f),
                    end = Offset(centerX, dotY),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            if (!isLast) {
                drawLine(
                    color = lineColor,
                    start = Offset(centerX, dotY),
                    end = Offset(centerX, size.height),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            drawCircle(
                color = Sage,
                radius = 6.dp.toPx(),
                center = Offset(centerX, dotY),
            )
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
            border = BorderStroke(1.dp, CardOutline),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleExpanded)
                        .clearAndSetSemantics {
                            contentDescription = "第 $stepNumber 张内容卡片，${acceptanceSummary(card)}，" +
                                if (expanded) "点击收起" else "点击展开"
                            role = Role.Button
                            onClick(label = if (expanded) "收起卡片" else "展开卡片") {
                                onToggleExpanded()
                                true
                            }
                        },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "第 $stepNumber 张卡片",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepBlue,
                        )
                        Text(
                            text = if (expanded) "收起" else "展开",
                            style = MaterialTheme.typography.labelLarge,
                            color = SageDark,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = acceptanceSummary(card),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (card.acceptanceCompleted) SageDark else MutedBlue,
                    )
                }

                if (expanded) {
                    Spacer(modifier = Modifier.height(14.dp))
                    DetailLabel("卡片目标")
                    Text(card.objective, style = MaterialTheme.typography.bodyLarge, color = DeepBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailLabel("初始动作")
                    Text(card.initialAction, style = MaterialTheme.typography.bodyLarge, color = DeepBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    DetailLabel("验收标准")
                    Text(card.acceptanceCriteria, style = MaterialTheme.typography.bodyLarge, color = DeepBlue)
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FilledTonalButton(
                            onClick = onOpenAcceptance,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("查看验收详情") }
                        Spacer(modifier = Modifier.height(8.dp))
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
        }
    }
}

private fun acceptanceSummary(card: TaskContentCard): String = when {
    !card.acceptanceCompleted -> "验收未完成"
    card.masteryLevel == null -> "已验收 · 请选择掌握度"
    else -> "已验收 · ${MasteryLevel.fromValue(card.masteryLevel)?.label ?: "掌握度无效"}"
}

@Composable
private fun DetailLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MutedBlue,
    )
    Spacer(modifier = Modifier.height(3.dp))
}

@Composable
private fun SmallBadge(
    text: String,
    background: Color,
    foreground: Color,
) {
    Surface(shape = RoundedCornerShape(9.dp), color = background) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
        )
    }
}

private sealed interface CardEditorTarget {
    data object Add : CardEditorTarget

    data class Edit(val card: TaskContentCard) : CardEditorTarget
}

@Composable
private fun CardEditorDialog(
    target: CardEditorTarget,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, (Boolean) -> Unit) -> Unit,
) {
    val existing = (target as? CardEditorTarget.Edit)?.card
    var objective by remember { mutableStateOf(existing?.objective.orEmpty()) }
    var initialAction by remember { mutableStateOf(existing?.initialAction.orEmpty()) }
    var criteria by remember { mutableStateOf(existing?.acceptanceCriteria.orEmpty()) }
    var objectiveError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var criteriaError by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = SurfaceWarm,
            tonalElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
            ) {
                Text(
                    text = if (existing == null) "添加内容卡片" else "编辑内容卡片",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepBlue,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = objective,
                    onValueChange = {
                        objective = it
                        objectiveError = if (it.isBlank()) "请填写卡片目标" else null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("卡片目标") },
                    minLines = 2,
                    isError = objectiveError != null,
                    supportingText = objectiveError?.let { { Text(it) } },
                    enabled = !isSaving,
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = initialAction,
                    onValueChange = {
                        initialAction = it
                        actionError = if (it.isBlank()) "请填写初始动作" else null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("初始动作") },
                    minLines = 2,
                    isError = actionError != null,
                    supportingText = actionError?.let { { Text(it) } },
                    enabled = !isSaving,
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = criteria,
                    onValueChange = {
                        criteria = it
                        criteriaError = if (it.isBlank()) "请填写验收标准" else null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("验收标准") },
                    minLines = 3,
                    isError = criteriaError != null,
                    supportingText = criteriaError?.let { { Text(it) } },
                    enabled = !isSaving,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss, enabled = !isSaving) { Text("取消") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            objectiveError = if (objective.isBlank()) "请填写卡片目标" else null
                            actionError = if (initialAction.isBlank()) "请填写初始动作" else null
                            criteriaError = if (criteria.isBlank()) "请填写验收标准" else null
                            if (
                                objectiveError == null &&
                                actionError == null &&
                                criteriaError == null
                            ) {
                                onSave(objective, initialAction, criteria) {}
                            }
                        },
                        enabled = !isSaving,
                    ) { Text("保存卡片") }
                }
            }
        }
    }
}

@Preview(name = "卡片详情", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun TaskDetailPreview() {
    val date = LocalDate.of(2026, 10, 25)
    val task = StudyTask(
        id = "task",
        title = "分数应用题复习",
        scheduledDate = date,
        startTime = LocalTime.of(9, 0),
        section = TaskSection.NEW,
        isCompleted = true,
        spacedRepetitionEnabled = false,
        createdDate = date,
        createdAtEpochMillis = 1,
        sourceType = TaskSourceType.ORIGINAL,
        sourceTaskId = null,
        reviewSequence = null,
    )
    val card = TaskContentCard(
        id = "card",
        taskId = task.id,
        objective = "理解单位一在分数应用题中的含义",
        initialAction = "圈出题目中的单位一并写出数量关系",
        acceptanceCriteria = "不看答案完成两道变式题，并讲清每一步为什么这样列式。",
        createdAtEpochMillis = 1,
        sortOrder = 0,
        acceptanceCompleted = true,
        masteryLevel = 0,
    )
    LearningCalendarTheme {
        TaskDetailScreen(
            state = TaskDetailUiState(task, listOf(card), false),
            isSaving = false,
            feedback = kotlinx.coroutines.flow.emptyFlow(),
            onBack = {},
            onOpenAcceptance = {},
            onUpdateTask = { _, _, _ -> },
            onTaskCompleted = { _, _, _ -> },
            onEnableRepetition = {},
            onCancelRepetition = {},
            onDeleteTask = {},
            onAddCard = { _, _, _, _ -> },
            onUpdateCard = { _, _, _, _, _ -> },
            onDeleteCard = { _, _ -> },
        )
    }
}
