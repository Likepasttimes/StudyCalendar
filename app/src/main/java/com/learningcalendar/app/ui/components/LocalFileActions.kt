package com.learningcalendar.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.learningcalendar.app.ui.DeepBlue
import com.learningcalendar.app.ui.MutedBlue
import com.learningcalendar.app.ui.Sage
import com.learningcalendar.app.ui.SurfaceWarm
import com.learningcalendar.app.ui.WarmBackground
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun openLocalFile(
    context: Context,
    localPath: String,
    mimeType: String?,
): Boolean {
    val file = File(localPath)
    if (!file.exists()) return false
    return try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType ?: "*/*")
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
fun LocalPhotoPreviewDialog(
    displayName: String,
    localPath: String,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    var loadFinished by remember(localPath) { mutableStateOf(false) }
    val bitmap by produceState<Bitmap?>(
        initialValue = null,
        key1 = localPath,
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching { decodeSampledBitmap(localPath) }.getOrNull()
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
                    text = displayName,
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
                    when {
                        image != null -> {
                            Image(
                                bitmap = image.asImageBitmap(),
                                contentDescription = contentDescription,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }

                        !loadFinished -> CircularProgressIndicator(color = Sage)
                        else -> Text("无法显示这张图片", color = MutedBlue)
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
