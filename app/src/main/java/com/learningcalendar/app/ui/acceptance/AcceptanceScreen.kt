package com.learningcalendar.app.ui.acceptance

import android.content.Intent
import android.content.Context
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.learningcalendar.app.domain.MasteryLevel
import com.learningcalendar.app.domain.LearningSnapshot
import com.learningcalendar.app.domain.SnapshotKind
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskContentCard
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.TaskSourceType
import com.learningcalendar.app.ui.CardOutline
import com.learningcalendar.app.ui.DeepBlue
import com.learningcalendar.app.ui.Amber
import com.learningcalendar.app.ui.LearningCalendarTheme
import com.learningcalendar.app.ui.MutedBlue
import com.learningcalendar.app.ui.Sage
import com.learningcalendar.app.ui.SageDark
import com.learningcalendar.app.ui.SageSoft
import com.learningcalendar.app.ui.SurfaceWarm
import com.learningcalendar.app.ui.WarmBackground
import java.io.File
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.media.MediaPlayer

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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var photoPreview by remember { mutableStateOf<LearningSnapshot?>(null) }
    var audioPreview by remember { mutableStateOf<LearningSnapshot?>(null) }
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
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            onImportSnapshot(uri, SnapshotKind.FILE)
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
                    onChooseFile = {
                        filePicker.launch(arrayOf("*/*"))
                    },
                    onOpenSnapshot = { snapshot ->
                        when (snapshot.kind) {
                            SnapshotKind.PHOTO -> photoPreview = snapshot
                            SnapshotKind.AUDIO -> audioPreview = snapshot
                            SnapshotKind.FILE -> {
                                val opened = openSnapshotWithSystemApp(context, snapshot)
                                if (!opened) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("没有可打开此文件的应用")
                                    }
                                }
                            }
                        }
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

    photoPreview?.let { snapshot ->
        SnapshotPhotoDialog(
            snapshot = snapshot,
            onDismiss = { photoPreview = null },
        )
    }
    audioPreview?.let { snapshot ->
        SnapshotAudioDialog(
            snapshot = snapshot,
            onDismiss = { audioPreview = null },
        )
    }
}

@Composable
private fun LearningSnapshotCard(
    snapshots: List<LearningSnapshot>,
    isSaving: Boolean,
    onChoosePhoto: () -> Unit,
    onChooseAudio: () -> Unit,
    onChooseFile: () -> Unit,
    onOpenSnapshot: (LearningSnapshot) -> Unit,
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
                        DropdownMenuItem(
                            text = { Text("上传文件") },
                            onClick = {
                                menuExpanded = false
                                onChooseFile()
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSnapshot(snapshot) }
                                .clearAndSetSemantics {
                                    contentDescription = "${snapshotKindLabel(snapshot.kind)}，" +
                                        "${snapshot.displayName}，点击打开"
                                    role = Role.Button
                                    onClick(label = "打开${snapshot.displayName}") {
                                        onOpenSnapshot(snapshot)
                                        true
                                    }
                                },
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
                                    color = when (snapshot.kind) {
                                        SnapshotKind.PHOTO -> SageSoft
                                        SnapshotKind.AUDIO -> Sage
                                        SnapshotKind.FILE -> SageSoft
                                    },
                                ) {
                                    Text(
                                        text = snapshotKindLabel(snapshot.kind),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (snapshot.kind == SnapshotKind.AUDIO) {
                                            WarmBackground
                                        } else {
                                            SageDark
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

private fun snapshotKindLabel(kind: SnapshotKind): String = when (kind) {
    SnapshotKind.PHOTO -> "照片"
    SnapshotKind.AUDIO -> "录音"
    SnapshotKind.FILE -> "文件"
}

private fun openSnapshotWithSystemApp(
    context: Context,
    snapshot: LearningSnapshot,
): Boolean {
    val file = File(snapshot.localPath)
    if (!file.exists()) return false
    return try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, snapshot.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(Intent.createChooser(intent, "打开文件"))
        true
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun SnapshotPhotoDialog(
    snapshot: LearningSnapshot,
    onDismiss: () -> Unit,
) {
    var loadFinished by remember(snapshot.localPath) { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = snapshot.localPath,
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching { decodeSampledBitmap(snapshot.localPath) }.getOrNull()
        }
        loadFinished = true
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceWarm,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = snapshot.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepBlue,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(WarmBackground, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = bitmap
                    if (image != null) {
                        Image(
                            bitmap = image.asImageBitmap(),
                            contentDescription = "学习照片：${snapshot.displayName}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    } else if (!loadFinished) {
                        CircularProgressIndicator(color = Sage)
                    } else {
                        Text(
                            text = "无法显示这张照片",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MutedBlue,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 48.dp),
                ) { Text("关闭") }
            }
        }
    }
}

@Composable
private fun SnapshotAudioDialog(
    snapshot: LearningSnapshot,
    onDismiss: () -> Unit,
) {
    var player by remember(snapshot.localPath) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember(snapshot.localPath) { mutableStateOf(false) }
    var durationMillis by remember(snapshot.localPath) { mutableIntStateOf(0) }
    var playbackError by remember(snapshot.localPath) { mutableStateOf<String?>(null) }

    DisposableEffect(snapshot.localPath) {
        val mediaPlayer = MediaPlayer()
        player = mediaPlayer
        try {
            mediaPlayer.setDataSource(snapshot.localPath)
            mediaPlayer.setOnPreparedListener { prepared ->
                durationMillis = prepared.duration.coerceAtLeast(0)
                prepared.start()
                isPlaying = true
            }
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                isPlaying = false
                playbackError = "无法播放该录音"
                true
            }
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            playbackError = "无法播放该录音"
        }

        onDispose {
            player = null
            mediaPlayer.release()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceWarm,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "录音播放",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepBlue,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = snapshot.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedBlue,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (playbackError != null) {
                    Text(
                        text = playbackError.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Amber,
                    )
                } else {
                    Text(
                        text = "时长 ${formatPlaybackDuration(durationMillis)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedBlue,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val mediaPlayer = player ?: return@FilledTonalButton
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.pause()
                                    isPlaying = false
                                } else {
                                    mediaPlayer.start()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            enabled = player != null,
                        ) {
                            Text(if (isPlaying) "暂停" else "播放")
                        }
                        OutlinedButton(
                            onClick = {
                                player?.let { mediaPlayer ->
                                    mediaPlayer.seekTo(0)
                                    mediaPlayer.start()
                                    isPlaying = true
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            enabled = player != null,
                        ) {
                            Text("从头播放")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.End)
                        .heightIn(min = 48.dp),
                ) { Text("关闭") }
            }
        }
    }
}

private fun formatPlaybackDuration(durationMillis: Int): String {
    val totalSeconds = durationMillis / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(Locale.SIMPLIFIED_CHINESE, minutes, seconds)
}

private fun decodeSampledBitmap(
    path: String,
    maxDimension: Int = 2048,
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > maxDimension ||
        bounds.outHeight / sampleSize > maxDimension
    ) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        },
    )
}

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
