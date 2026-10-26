package com.learningcalendar.app.di

import android.content.Context
import androidx.room.Room
import com.learningcalendar.app.data.RoomStudyTaskRepository
import com.learningcalendar.app.data.StudyTaskRepository
import com.learningcalendar.app.data.local.LearningCalendarDatabase
import com.learningcalendar.app.data.local.LocalSnapshotStorage
import com.learningcalendar.app.data.sync.ComputerSyncAutoSync
import com.learningcalendar.app.data.sync.ComputerSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: LearningCalendarDatabase = Room.databaseBuilder(
        context.applicationContext,
        LearningCalendarDatabase::class.java,
        "learning_calendar.db",
    ).addMigrations(
        LearningCalendarDatabase.MIGRATION_1_2,
        LearningCalendarDatabase.MIGRATION_2_3,
    ).build()

    private val snapshotStorage = LocalSnapshotStorage(context.applicationContext)

    val taskRepository: StudyTaskRepository = RoomStudyTaskRepository(
        database = database,
        snapshotStorage = snapshotStorage,
    )

    val computerSyncRepository = ComputerSyncRepository(
        context = context.applicationContext,
        database = database,
        snapshotStorage = snapshotStorage,
    )

    val computerSyncAutoSync = ComputerSyncAutoSync(
        database = database,
        repository = computerSyncRepository,
        scope = applicationScope,
    )

    init {
        applicationScope.launch {
            taskRepository.seedIfNeeded()
        }
    }
}
