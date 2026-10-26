package com.learningcalendar.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

enum class MainSection {
    CALENDAR,
    WEEKLY_SUMMARY,
    COMPUTER_SYNC,
}

@Composable
fun MainSectionBar(
    selected: MainSection,
    onCalendarClick: () -> Unit,
    onWeeklySummaryClick: () -> Unit,
    onComputerSyncClick: () -> Unit,
) {
    NavigationBar(
        containerColor = SurfaceWarm,
        contentColor = DeepBlue,
    ) {
        NavigationBarItem(
            selected = selected == MainSection.CALENDAR,
            onClick = onCalendarClick,
            icon = { Text("日", style = MaterialTheme.typography.titleMedium) },
            label = { Text("学习日历") },
        )
        NavigationBarItem(
            selected = selected == MainSection.WEEKLY_SUMMARY,
            onClick = onWeeklySummaryClick,
            icon = { Text("周", style = MaterialTheme.typography.titleMedium) },
            label = { Text("周总结") },
        )
        NavigationBarItem(
            selected = selected == MainSection.COMPUTER_SYNC,
            onClick = onComputerSyncClick,
            icon = { Text("联", style = MaterialTheme.typography.titleMedium) },
            label = { Text("电脑同步") },
        )
    }
}
