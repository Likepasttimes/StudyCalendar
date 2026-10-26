package com.learningcalendar.app.data.sync

import androidx.room.InvalidationTracker
import com.learningcalendar.app.data.local.LearningCalendarDatabase
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ComputerSyncAutoSync(
    database: LearningCalendarDatabase,
    private val repository: ComputerSyncRepository,
    private val scope: CoroutineScope,
) {
    private val pendingSync = AtomicReference<Job?>(null)

    init {
        database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(SYNCED_TABLES) {
                override fun onInvalidated(tables: Set<String>) {
                    schedule()
                }
            },
        )
        schedule()
    }

    private fun schedule() {
        pendingSync.getAndSet(
            scope.launch {
                delay(AUTO_SYNC_DEBOUNCE_MILLIS)
                repository.autoSyncIfConfigured()
            },
        )?.cancel()
    }

    private companion object {
        const val AUTO_SYNC_DEBOUNCE_MILLIS = 1_500L
        val SYNCED_TABLES = arrayOf(
            "study_tasks",
            "task_content_cards",
            "learning_snapshots",
            "error_records",
            "error_screenshots",
            "week_summary_documents",
        )
    }
}
