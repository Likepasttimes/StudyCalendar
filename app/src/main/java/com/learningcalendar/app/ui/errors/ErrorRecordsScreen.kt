package com.learningcalendar.app.ui.errors

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.learningcalendar.app.domain.ErrorRecord
import com.learningcalendar.app.domain.ErrorScreenshot
import com.learningcalendar.app.domain.SnapshotKind
import com.learningcalendar.app.ui.Amber
import com.learningcalendar.app.ui.CardOutline
import com.learningcalendar.app.ui.DeepBlue
import com.learningcalendar.app.ui.MutedBlue
import com.learningcalendar.app.ui.Sage
import com.learningcalendar.app.ui.SageDark
import com.learningcalendar.app.ui.SageSoft
import com.learningcalendar.app.ui.SurfaceWarm
import com.learningcalendar.app.ui.WarmBackground
import com.learningcalendar.app.ui.components.LocalPhotoPreviewDialog
import com.learningcalendar.app.ui.components.openLocalFile
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext

@Composable
fun ErrorRecordsScreen(
    state: ErrorRecordsUiState,
    isSaving: Boolean,
    feedback: kotlinx.coroutines.flow.Flow<com.learningcalendar.app.ui.UiFeedback>,
    onBack: () -> Unit,
    onCreate: (String, String, List<Uri>, List<Uri>, (Boolean) -> Unit) -> Unit,
    onUpdate: (String, String, String, (Boolean) -> Unit) -> Unit,
    onDelete: (String, (Boolean) -> Unit) -> Unit,
    onUploadAttachment: (String, Uri, SnapshotKind, (Boolean) -> Unit) -> Unit,
    onDeleteScreenshot: (String, (Boolean) -> Unit) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var editorRecord by remember { mutableStateOf<ErrorRecord?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<ErrorRecord?>(null) }
    var attachmentTarget by remember { mutableStateOf<String?>(null) }
    var draftPhotoUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var draftFileUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var previewAttachment by remember { mutableStateOf<ErrorScreenshot?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val target = attachmentTarget
            if (target == null) {
                draftPhotoUris = draftPhotoUris + uri
            } else {
                onUploadAttachment(target, uri, SnapshotKind.PHOTO) {}
            }
        }
        attachmentTarget = null
    }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val target = attachmentTarget
            if (target == null) {
                draftFileUris = draftFileUris + uri
            } else {
                onUploadAttachment(target, uri, SnapshotKind.FILE) {}
            }
        }
        attachmentTarget = null
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
                    ) { Text("← 返回验收详情") }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "错误记录",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepBlue,
                    )
                    state.card?.let { card ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "内容卡片：${card.objective}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedBlue,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        onClick = {
                            draftPhotoUris = emptyList()
                            draftFileUris = emptyList()
                            editorRecord = null
                            editorOpen = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        enabled = !isSaving && state.card != null,
                        shape = RoundedCornerShape(15.dp),
                    ) {
                        Text("新建错误记录卡片")
                    }
                }
            }

            if (!state.isLoading && state.records.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "还没有错误记录卡片",
                            color = MutedBlue,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            state.records.forEachIndexed { index, item ->
                item(key = item.record.id) {
                    ErrorRecordCard(
                        sequence = index + 1,
                        item = item,
                        isSaving = isSaving,
                        onEdit = {
                            editorRecord = item.record
                            editorOpen = true
                        },
                        onDelete = { deleteTarget = item.record },
                        onAddPhoto = {
                            attachmentTarget = item.record.id
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onAddFile = {
                            attachmentTarget = item.record.id
                            filePicker.launch(arrayOf("*/*"))
                        },
                        onOpenAttachment = { attachment ->
                            if (attachment.mimeType?.startsWith("image/") == true) {
                                previewAttachment = attachment
                            } else if (!openLocalFile(context, attachment.localPath, attachment.mimeType)) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("没有可打开此文件的应用")
                                }
                            }
                        },
                        onDeleteScreenshot = { onDeleteScreenshot(it.id) {} },
                    )
                }
            }
        }
    }

    if (editorOpen) {
        ErrorRecordEditorSheet(
            record = editorRecord,
            isSaving = isSaving,
            onDismiss = {
                if (!isSaving) {
                    draftPhotoUris = emptyList()
                    draftFileUris = emptyList()
                    editorOpen = false
                }
            },
            draftPhotoUris = draftPhotoUris,
            draftFileUris = draftFileUris,
            onAddPhoto = {
                attachmentTarget = null
                photoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onAddFile = {
                attachmentTarget = null
                filePicker.launch(arrayOf("*/*"))
            },
            onSave = { point, description, result ->
                val current = editorRecord
                if (current == null) {
                    onCreate(point, description, draftPhotoUris, draftFileUris) { success ->
                        result(success)
                        if (success) {
                            draftPhotoUris = emptyList()
                            draftFileUris = emptyList()
                            editorOpen = false
                        }
                    }
                } else {
                    onUpdate(current.id, point, description) { success ->
                        result(success)
                        if (success) editorOpen = false
                    }
                }
            },
        )
    }

    deleteTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { if (!isSaving) deleteTarget = null },
            title = { Text("删除错误记录卡片？") },
            text = { Text("卡片内容及其截图会一并删除，无法恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(record.id) { success ->
                            if (success) deleteTarget = null
                        }
                    },
                    enabled = !isSaving,
                ) { Text("确认删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
            containerColor = SurfaceWarm,
        )
    }

    previewAttachment?.let { screenshot ->
        LocalPhotoPreviewDialog(
            displayName = screenshot.displayName,
            localPath = screenshot.localPath,
            contentDescription = "错误记录截图：${screenshot.displayName}",
            onDismiss = { previewAttachment = null },
        )
    }
}

@Composable
private fun ErrorRecordCard(
    sequence: Int,
    item: ErrorRecordCardUi,
    isSaving: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddPhoto: () -> Unit,
    onAddFile: () -> Unit,
    onOpenAttachment: (ErrorScreenshot) -> Unit,
    onDeleteScreenshot: (ErrorScreenshot) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
        border = BorderStroke(1.dp, CardOutline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "错误记录卡片 $sequence",
                style = MaterialTheme.typography.titleMedium,
                color = DeepBlue,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("错误点", style = MaterialTheme.typography.labelMedium, color = MutedBlue)
            Text(item.record.errorPoint, style = MaterialTheme.typography.bodyLarge, color = DeepBlue)
            if (item.record.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("错误描述", style = MaterialTheme.typography.labelMedium, color = MutedBlue)
                Text(item.record.description, style = MaterialTheme.typography.bodyLarge, color = DeepBlue)
            }

            if (item.screenshots.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    item.screenshots.forEach { screenshot ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onOpenAttachment(screenshot) }
                                .clearAndSetSemantics {
                                    contentDescription = "打开附件 ${screenshot.displayName}"
                                    role = Role.Button
                                    onClick(label = "打开附件") {
                                        onOpenAttachment(screenshot)
                                        true
                                    }
                                },
                            shape = RoundedCornerShape(13.dp),
                            color = WarmBackground,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "附件 · ${screenshot.displayName}",
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepBlue,
                                )
                                TextButton(
                                    onClick = { onDeleteScreenshot(screenshot) },
                                    enabled = !isSaving,
                                ) { Text("删除", color = Amber) }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onAddPhoto,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                ) { Text("上传照片") }
                OutlinedButton(
                    onClick = onAddFile,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                ) { Text("上传文件") }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                    border = BorderStroke(1.dp, Amber),
                ) { Text("删除卡片", color = Amber) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorRecordEditorSheet(
    record: ErrorRecord?,
    isSaving: Boolean,
    draftPhotoUris: List<Uri>,
    draftFileUris: List<Uri>,
    onAddPhoto: () -> Unit,
    onAddFile: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, (Boolean) -> Unit) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !isSaving },
    )
    var errorPoint by remember(record?.id) { mutableStateOf(record?.errorPoint.orEmpty()) }
    var description by remember(record?.id) { mutableStateOf(record?.description.orEmpty()) }
    var pointError by remember(record?.id) { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = SurfaceWarm,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                text = if (record == null) "新建错误记录卡片" else "编辑错误记录卡片",
                style = MaterialTheme.typography.titleLarge,
                color = DeepBlue,
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedTextField(
                value = errorPoint,
                onValueChange = {
                    errorPoint = it
                    pointError = if (it.isBlank()) "请填写错误点" else null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("错误点（必填）") },
                minLines = 2,
                isError = pointError != null,
                supportingText = pointError?.let { { Text(it) } },
                enabled = !isSaving,
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("错误描述（选填）") },
                minLines = 4,
                enabled = !isSaving,
            )
            if (record == null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "附件（可选）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedBlue,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onAddPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        enabled = !isSaving,
                    ) { Text("上传照片") }
                    OutlinedButton(
                        onClick = onAddFile,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        enabled = !isSaving,
                    ) { Text("上传文件") }
                }
                if (draftPhotoUris.isNotEmpty() || draftFileUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "已选择 ${draftPhotoUris.size} 张照片、" +
                            "${draftFileUris.size} 个文件",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SageDark,
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
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
                ) { Text("取消") }
                Button(
                    onClick = {
                        pointError = if (errorPoint.isBlank()) "请填写错误点" else null
                        if (pointError == null) {
                            onSave(errorPoint.trim(), description.trim()) {}
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    enabled = !isSaving,
                ) { Text("保存") }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
