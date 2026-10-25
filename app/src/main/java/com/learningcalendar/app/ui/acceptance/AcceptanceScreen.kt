package com.learningcalendar.app.ui.acceptance

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.learningcalendar.app.domain.MasteryLevel
import com.learningcalendar.app.domain.LearningSnapshot
import com.learningcalendar.app.domain.SnapshotKind
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskContentCard
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.TaskSourceType
import com.learningcalendar.app.ui.CardOutline
import com.learningcalendar.app.ui.DeepBlue
import com.learningcalendar.app.ui.LearningCalendarTheme
import com.learningcalendar.app.ui.MutedBlue
import com.learningcalendar.app.ui.Sage
import com.learningcalendar.app.ui.SageDark
import com.learningcalendar.app.ui.SageSoft
import com.learningcalendar.app.ui.SurfaceWarm
import com.learningcalendar.app.ui.WarmBackground
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AcceptanceScreen(
    state: AcceptanceUiState,
    isSaving: Boolean,
    feedback: kotlinx.coroutines.flow.Flow<com.learningcalendar.app.ui.UiFeedback>,
    onBack: () -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onMasterySelected: (Int) -> Unit,
    onImportSnapshot: (Uri, SnapshotKind) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onImportSnapshot(uri, SnapshotKind.PHOTO)
        }
    }
    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            onImportSnapshot(uri, SnapshotKind.AUDIO)
        }
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

        val card = state.card
        val task = state.task
        if (card == null) {
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
            contentPadding = PaddingValues(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "back") {
                TextButton(
                    onClick = onBack,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp),
                ) { Text("← 返回卡片详情") }
            }
            item(key = "context") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SageSoft),
                    border = BorderStroke(1.dp, CardOutline),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "验收详情",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepBlue,
                        )
                        Spacer(modifier = Modifier.height(9.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = SurfaceWarm,
                            ) {
                                Text(
                                    text = "任务卡片",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SageDark,
                                )
                            }
                            Text(
                                text = task?.title ?: "卡片信息不可用",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepBlue,
                            )
                        }
                        if (task != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${task.scheduledDate.year} 年 " +
                                    "${task.scheduledDate.monthValue} 月 " +
                                    "${task.scheduledDate.dayOfMonth} 日",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedBlue,
                            )
                        }
                    }
                }
            }
            item(key = "criteria") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    border = BorderStroke(1.dp, CardOutline),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "验收标准",
                            style = MaterialTheme.typography.labelLarge,
                            color = MutedBlue,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = card.acceptanceCriteria,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DeepBlue,
                        )
                    }
                }
            }
            item(key = "learning-snapshots") {
                LearningSnapshotCard(
                    snapshots = state.snapshots,
                    isSaving = isSaving,
                    onChoosePhoto = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    onChooseAudio = {
                        audioPicker.launch(arrayOf("audio/*"))
                    },
                )
            }
            item(key = "completion") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    border = BorderStroke(1.dp, CardOutline),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(enabled = !isSaving) {
                                    onCompletedChange(!card.acceptanceCompleted)
                                }
                                .clearAndSetSemantics {
                                    contentDescription = if (card.acceptanceCompleted) {
                                        "验收已完成，点击取消完成"
                                    } else {
                                        "验收未完成，点击标记完成"
                                    }
                                    role = Role.Checkbox
                                    onClick(label = "切换验收完成") {
                                        onCompletedChange(!card.acceptanceCompleted)
                                        true
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = card.acceptanceCompleted,
                                onCheckedChange = null,
                                enabled = !isSaving,
                            )
                            Text(
                                text = if (card.acceptanceCompleted) "完成" else "标记为完成",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepBlue,
                            )
                        }

                        if (!card.acceptanceCompleted) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "完成验收后即可选择掌握度",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedBlue,
                            )
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "掌握度",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepBlue,
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (card.masteryLevel == null) {
                                    "请选择掌握度"
                                } else {
                                    "已保存：${MasteryLevel.fromValue(card.masteryLevel)?.label}"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (card.masteryLevel == null) MutedBlue else SageDark,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MasteryLevel.entries.forEach { level ->
                                MasteryChoice(
                                    level = level,
                                    selected = card.masteryLevel == level.value,
                                    enabled = !isSaving,
                                    onSelect = { onMasterySelected(level.value) },
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "选择 0–4 中任意等级后自动保存到本机",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedBlue,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningSnapshotCard(
    snapshots: List<LearningSnapshot>,
    isSaving: Boolean,
    onChoosePhoto: () -> Unit,
    onChooseAudio: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, CardOutline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    FilledTonalButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.heightIn(min = 48.dp),
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("学习快照")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("上传照片") },
                            onClick = {
                                menuExpanded = false
                                onChoosePhoto()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("上传录音") },
                            onClick = {
                                menuExpanded = false
                                onChooseAudio()
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${snapshots.size} 条",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedBlue,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (snapshots.isEmpty()) {
                Text(
                    text = "暂无学习快照",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedBlue,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshots.forEach { snapshot ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            color = WarmBackground,
                            border = BorderStroke(1.dp, CardOutline),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (snapshot.kind == SnapshotKind.PHOTO) {
                                        SageSoft
                                    } else {
                                        Sage
                                    },
                                ) {
                                    Text(
                                        text = if (snapshot.kind == SnapshotKind.PHOTO) {
                                            "照片"
                                        } else {
                                            "录音"
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (snapshot.kind == SnapshotKind.PHOTO) {
                                            SageDark
                                        } else {
                                            WarmBackground
                                        },
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = snapshot.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepBlue,
                                    )
                                    Text(
                                        text = snapshotTimeFormatter.format(
                                            Instant.ofEpochMilli(snapshot.createdAtEpochMillis)
                                                .atZone(ZoneId.systemDefault()),
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MutedBlue,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val snapshotTimeFormatter =
    DateTimeFormatter.ofPattern("M 月 d 日 HH:mm", Locale.SIMPLIFIED_CHINESE)

@Composable
private fun MasteryChoice(
    level: MasteryLevel,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 2.dp)
            .clickable(enabled = enabled, onClick = onSelect)
            .clearAndSetSemantics {
                contentDescription = "掌握度 ${level.value}，${level.label}"
                this.selected = selected
                role = Role.RadioButton
                onClick(label = "选择${level.label}") {
                    onSelect()
                    true
                }
            },
        shape = shape,
        color = if (selected) SageSoft else SurfaceWarm,
        border = BorderStroke(1.dp, if (selected) Sage else CardOutline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                enabled = enabled,
            )
            Text(
                text = "${level.value}  ${level.label}",
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) SageDark else DeepBlue,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Preview(name = "验收详情", widthDp = 360, heightDp = 900, showBackground = true)
@Composable
private fun AcceptancePreview() {
    val date = LocalDate.of(2026, 10, 25)
    val task = StudyTask(
        id = "task",
        title = "分数应用题复习",
        scheduledDate = date,
        startTime = null,
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
        objective = "理解单位一",
        initialAction = "圈出单位一并写出数量关系",
        acceptanceCriteria = "不看答案完成两道变式题，并讲清每一步为什么这样列式。",
        createdAtEpochMillis = 1,
        sortOrder = 0,
        acceptanceCompleted = true,
        masteryLevel = 0,
    )
    LearningCalendarTheme {
        AcceptanceScreen(
            state = AcceptanceUiState(card, task, emptyList(), false),
            isSaving = false,
            feedback = kotlinx.coroutines.flow.emptyFlow(),
            onBack = {},
            onCompletedChange = {},
            onMasterySelected = {},
            onImportSnapshot = { _, _ -> },
        )
    }
}
