package com.learningcalendar.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.TaskSourceType
import com.learningcalendar.app.ui.DeepBlue
import com.learningcalendar.app.ui.MutedBlue
import com.learningcalendar.app.ui.SurfaceWarm
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    task: StudyTask?,
    section: TaskSection,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, LocalTime?, (Boolean) -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isSaving },
    )
    var title by remember(task?.id, section) { mutableStateOf(task?.title.orEmpty()) }
    var timeText by remember(task?.id, section) {
        mutableStateOf(task?.startTime?.format(hourMinuteFormatter).orEmpty())
    }
    var titleError by remember(task?.id, section) { mutableStateOf<String?>(null) }
    var timeError by remember(task?.id, section) { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = SurfaceWarm,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = when {
                    task != null -> "编辑卡片"
                    section == TaskSection.NEW -> "新建今天的任务卡片"
                    else -> "新建复习卡片"
                },
                style = MaterialTheme.typography.titleLarge,
                color = DeepBlue,
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = if (it.isBlank()) "请输入卡片标题" else null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("卡片标题（必填）") },
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it) } },
                enabled = !isSaving,
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = timeText,
                onValueChange = {
                    timeText = it
                    timeError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("开始时间（可选）") },
                placeholder = { Text("例如 09:30") },
                isError = timeError != null,
                supportingText = timeError?.let { { Text(it) } },
                enabled = !isSaving,
                singleLine = true,
            )
            if (task?.sourceType == TaskSourceType.ORIGINAL) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "创建日期：${task.createdDate}（不可修改）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedBlue,
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                ) { Text("取消") }
                Button(
                    onClick = {
                        val normalizedTitle = title.trim()
                        titleError = if (normalizedTitle.isEmpty()) "请输入卡片标题" else null
                        val parsedTime = parseTime(timeText)
                        timeError = if (timeText.isNotBlank() && parsedTime == null) {
                            "请使用 09:30 或 9:30 格式"
                        } else {
                            null
                        }
                        if (titleError == null && timeError == null) {
                            onSave(normalizedTitle, parsedTime) {}
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                ) { Text("保存") }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun parseTime(raw: String): LocalTime? {
    if (raw.isBlank()) return null
    return listOf(
        DateTimeFormatter.ISO_LOCAL_TIME,
        DateTimeFormatter.ofPattern("H:mm"),
    ).firstNotNullOfOrNull { formatter ->
        try {
            LocalTime.parse(raw.trim(), formatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

private val hourMinuteFormatter = DateTimeFormatter.ofPattern("HH:mm")
