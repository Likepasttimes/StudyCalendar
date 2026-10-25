package com.learningcalendar.app.di

import android.content.Context
import androidx.room.Room
import com.learningcalendar.app.data.RoomStudyTaskRepository
import com.learningcalendar.app.data.StudyTaskRepository
import com.learningcalendar.app.data.local.LearningCalendarDatabase
import com.learningcalendar.app.data.local.LocalSnapshotStorage
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
    ).addMigrations(LearningCalendarDatabase.MIGRATION_1_2).build()

    val taskRepository: StudyTaskRepository = RoomStudyTaskRepository(
        database = database,
        snapshotStorage = LocalSnapshotStorage(context.applicationContext),
    )

    init {
        applicationScope.launch {
            taskRepository.seedIfNeeded()
        }
    }
}
