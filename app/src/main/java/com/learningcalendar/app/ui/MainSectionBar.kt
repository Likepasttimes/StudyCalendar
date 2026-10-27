package com.learningcalendar.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class MainSection {
    CALENDAR,
    WEEKLY_SUMMARY,
    COMPUTER_SYNC,
    OVERVIEW,
}

@Composable
fun MainSectionBar(
    selected: MainSection,
    onCalendarClick: () -> Unit,
    onWeeklySummaryClick: () -> Unit,
    onComputerSyncClick: () -> Unit,
    onOverviewClick: () -> Unit,
) {
    NavigationBar(
        containerColor = SurfaceWarm,
        contentColor = DeepBlue,
    ) {
        NavigationBarItem(
            selected = selected == MainSection.CALENDAR,
            onClick = onCalendarClick,
            icon = { CalendarTabIcon() },
        )
        NavigationBarItem(
            selected = selected == MainSection.WEEKLY_SUMMARY,
            onClick = onWeeklySummaryClick,
            icon = { WeeklyTabIcon() },
        )
        NavigationBarItem(
            selected = selected == MainSection.COMPUTER_SYNC,
            onClick = onComputerSyncClick,
            icon = { SyncTabIcon() },
        )
        NavigationBarItem(
            selected = selected == MainSection.OVERVIEW,
            onClick = onOverviewClick,
            icon = { OverviewTabIcon() },
        )
    }
}

@Composable
private fun CalendarTabIcon() {
    val color = LocalContentColor.current
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics { contentDescription = "学习日历" },
    ) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.18f, size.height * 0.22f),
            size = Size(size.width * 0.64f, size.height * 0.62f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.18f, size.height * 0.42f),
            end = Offset(size.width * 0.82f, size.height * 0.42f),
            strokeWidth = 1.8.dp.toPx(),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.34f, size.height * 0.13f),
            end = Offset(size.width * 0.34f, size.height * 0.29f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.66f, size.height * 0.13f),
            end = Offset(size.width * 0.66f, size.height * 0.29f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        listOf(0.36f, 0.50f, 0.64f).forEach { x ->
            drawCircle(
                color = color,
                radius = 1.25.dp.toPx(),
                center = Offset(size.width * x, size.height * 0.58f),
            )
            drawCircle(
                color = color,
                radius = 1.25.dp.toPx(),
                center = Offset(size.width * x, size.height * 0.72f),
            )
        }
    }
}

@Composable
private fun WeeklyTabIcon() {
    val color = LocalContentColor.current
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics { contentDescription = "周总结" },
    ) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.20f, size.height * 0.14f),
            size = Size(size.width * 0.60f, size.height * 0.72f),
            cornerRadius = CornerRadius(3.dp.toPx()),
            style = stroke,
        )
        listOf(0.35f, 0.50f, 0.65f).forEach { y ->
            drawLine(
                color = color,
                start = Offset(size.width * 0.36f, size.height * y),
                end = Offset(size.width * 0.68f, size.height * y),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
        val check = Path().apply {
            moveTo(size.width * 0.27f, size.height * 0.73f)
            lineTo(size.width * 0.35f, size.height * 0.81f)
            lineTo(size.width * 0.48f, size.height * 0.66f)
        }
        drawPath(check, color = color, style = stroke)
    }
}

@Composable
private fun SyncTabIcon() {
    val color = LocalContentColor.current
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics { contentDescription = "电脑同步" },
    ) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.13f, size.height * 0.19f),
            size = Size(size.width * 0.74f, size.height * 0.50f),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.50f, size.height * 0.69f),
            end = Offset(size.width * 0.50f, size.height * 0.83f),
            strokeWidth = 1.8.dp.toPx(),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.36f, size.height * 0.83f),
            end = Offset(size.width * 0.64f, size.height * 0.83f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawArc(
            color = color,
            startAngle = 205f,
            sweepAngle = 115f,
            useCenter = false,
            topLeft = Offset(size.width * 0.29f, size.height * 0.30f),
            size = Size(size.width * 0.42f, size.height * 0.32f),
            style = stroke,
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.68f, size.height * 0.37f),
            end = Offset(size.width * 0.73f, size.height * 0.45f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun OverviewTabIcon() {
    val color = LocalContentColor.current
    Canvas(
        modifier = Modifier
            .size(24.dp)
            .semantics { contentDescription = "任务完成概况" },
    ) {
        val radius = size.minDimension * 0.36f
        drawCircle(
            color = color,
            radius = radius,
            style = Stroke(width = 1.8.dp.toPx()),
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x, center.y - radius),
            strokeWidth = 1.8.dp.toPx(),
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x + radius * 0.88f, center.y + radius * 0.50f),
            strokeWidth = 1.8.dp.toPx(),
        )
        drawLine(
            color = color,
            start = center,
            end = Offset(center.x - radius * 0.88f, center.y + radius * 0.50f),
            strokeWidth = 1.8.dp.toPx(),
        )
    }
}
