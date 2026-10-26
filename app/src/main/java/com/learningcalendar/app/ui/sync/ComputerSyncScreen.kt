package com.learningcalendar.app.ui.sync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.learningcalendar.app.ui.Amber
import com.learningcalendar.app.ui.CardOutline
import com.learningcalendar.app.ui.DeepBlue
import com.learningcalendar.app.ui.MutedBlue
import com.learningcalendar.app.ui.SageDark
import com.learningcalendar.app.ui.SageSoft
import com.learningcalendar.app.ui.SurfaceWarm
import com.learningcalendar.app.ui.WarmBackground

@Composable
fun ComputerSyncScreen(
    state: ComputerSyncUiState,
    bottomBar: @Composable () -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onPairingCodeChange: (String) -> Unit,
    onSync: () -> Unit,
) {
    val context = LocalContext.current
    var permissionMessage by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            onSync()
        } else {
            permissionMessage = "需要允许访问本地网络，才能连接同一 Wi-Fi 下的电脑"
        }
    }

    val requestSync = {
        val needsLocalNetworkPermission = Build.VERSION.SDK_INT >= 37 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_LOCAL_NETWORK,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsLocalNetworkPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
        } else {
            permissionMessage = null
            onSync()
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
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 18.dp,
                top = 18.dp,
                end = 18.dp,
                bottom = 28.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = "电脑同步",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepBlue,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "让手机和电脑连接同一 Wi-Fi，在电脑版中查看地址和配对码。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedBlue,
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWarm),
                    border = BorderStroke(1.dp, CardOutline),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "连接电脑",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepBlue,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = state.host,
                            onValueChange = onHostChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("电脑地址") },
                            placeholder = { Text("例如 192.168.1.20") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            enabled = !state.isSyncing,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            OutlinedTextField(
                                value = state.port,
                                onValueChange = onPortChange,
                                modifier = Modifier.weight(1f),
                                label = { Text("端口") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                ),
                                enabled = !state.isSyncing,
                            )
                            OutlinedTextField(
                                value = state.pairingCode,
                                onValueChange = onPairingCodeChange,
                                modifier = Modifier.weight(2f),
                                label = { Text("6 位配对码") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                ),
                                enabled = !state.isSyncing,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = requestSync,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            enabled = !state.isSyncing,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            if (state.isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                        .size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                                Text("正在打包并同步…")
                            } else {
                                Text("同步全部数据到电脑")
                            }
                        }
                    }
                }
            }

            state.resultMessage?.let { message ->
                item {
                    SyncMessageCard(
                        title = "同步成功",
                        message = message,
                        containerColor = SageSoft,
                        contentColor = SageDark,
                    )
                }
            }
            (state.errorMessage ?: permissionMessage)?.let { message ->
                item {
                    SyncMessageCard(
                        title = "暂时无法同步",
                        message = message,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = Amber,
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SageSoft),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "同步范围",
                            style = MaterialTheme.typography.titleMedium,
                            color = DeepBlue,
                        )
                        Text(
                            text = "全部任务卡、内容卡、完成状态、五级掌握度、" +
                                "学习快照、错误记录、周总结，以及照片、录音和文件。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeepBlue,
                        )
                        Text(
                            text = "数据只上传到当前电脑，不经过云端。电脑端会保留完整同步快照。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedBlue,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncMessageCard(
    title: String,
    message: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = DeepBlue,
            )
        }
    }
}
