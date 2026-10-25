package com.learningcalendar.app.data

import android.net.Uri
import androidx.room.withTransaction
import com.learningcalendar.app.data.local.AppMetadataEntity
import com.learningcalendar.app.data.local.LearningCalendarDatabase
import com.learningcalendar.app.data.local.LearningSnapshotEntity
import com.learningcalendar.app.data.local.LocalSnapshotStorage
import com.learningcalendar.app.data.local.StudyTaskEntity
import com.learningcalendar.app.data.local.TaskContentCardEntity
import com.learningcalendar.app.domain.ReviewSchedule
import com.learningcalendar.app.domain.LearningSnapshot
import com.learningcalendar.app.domain.SnapshotKind
import com.learningcalendar.app.domain.StudyTask
import com.learningcalendar.app.domain.TaskContentCard
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.TaskSourceType
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomStudyTaskRepository(
    private val database: LearningCalendarDatabase,
    private val snapshotStorage: LocalSnapshotStorage? = null,
    private val todayProvider: () -> LocalDate = LocalDate::now,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) : StudyTaskRepository {
    private val dao = database.studyTaskDao()

    override fun observeTaskCounts(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<Map<LocalDate, Int>> =
        dao.observeTaskCounts(startDate.toString(), endDate.toString()).map { rows ->
            rows.associate { row -> LocalDate.parse(row.date) to row.taskCount }
        }

    override fun observeTasksForDate(date: LocalDate): Flow<List<StudyTask>> =
        dao.observeTasksForDate(date.toString()).map { tasks -> tasks.map(StudyTaskEntity::toDomain) }

    override fun observeTask(taskId: String): Flow<StudyTask?> =
        dao.observeTask(taskId).map { it?.toDomain() }

    override fun observeCards(taskId: String): Flow<List<TaskContentCard>> =
        dao.observeCards(taskId).map { cards ->
            cards.map(TaskContentCardEntity::toDomain)
        }

    override fun observeCard(cardId: String): Flow<TaskContentCard?> =
        dao.observeCard(cardId).map { it?.toDomain() }

    override fun observeSnapshots(cardId: String): Flow<List<LearningSnapshot>> =
        dao.observeSnapshots(cardId).map { snapshots ->
            snapshots.map { snapshot ->
                snapshot.toDomain(
                    localPath = snapshotStorage?.resolvePath(snapshot.relativePath)
                        ?: snapshot.relativePath,
                )
            }
        }

    override suspend fun seedIfNeeded() {
        database.withTransaction {
            if (dao.getMetadata(SEED_METADATA_KEY) != null) {
                return@withTransaction
            }

            if (dao.taskCount() == 0) {
                seedTasks(todayProvider())
            }
            dao.putMetadata(AppMetadataEntity(SEED_METADATA_KEY, "done"))
        }
    }

    override suspend fun createTask(
        title: String,
        scheduledDate: LocalDate,
        startTime: LocalTime?,
        section: TaskSection,
    ): String {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "卡片标题不能为空" }

        return database.withTransaction {
            val taskId = UUID.randomUUID().toString()
            val now = nowProvider()
            dao.insertTask(
                StudyTaskEntity(
                    id = taskId,
                    title = normalizedTitle,
                    scheduledDate = scheduledDate.toString(),
                    startTime = startTime?.toString(),
                    section = section.name,
                    isCompleted = false,
                    spacedRepetitionEnabled = false,
                    createdDate = todayProvider().toString(),
                    createdAtEpochMillis = now,
                    sourceType = if (section == TaskSection.NEW) {
                        TaskSourceType.ORIGINAL.name
                    } else {
                        TaskSourceType.MANUAL_REVIEW.name
                    },
                    sourceTaskId = null,
                    reviewSequence = null,
                ),
            )
            taskId
        }
    }

    override suspend fun updateTask(
        taskId: String,
        title: String,
        startTime: LocalTime?,
    ) {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "卡片标题不能为空" }
        dao.updateTaskFields(taskId, normalizedTitle, startTime?.toString())
    }

    override suspend fun deleteTask(taskId: String) {
        val snapshotFiles = mutableListOf<String>()
        snapshotFiles += dao.getSnapshotsForTask(taskId)
            .map(LearningSnapshotEntity::relativePath)
        ReviewSchedule.reviewSequences.forEach { sequence ->
            val review = dao.findAutoReview(taskId, sequence)
            if (review != null) {
                snapshotFiles += dao.getSnapshotsForTask(review.id)
                    .map(LearningSnapshotEntity::relativePath)
            }
        }
        dao.deleteTask(taskId)
        snapshotStorage?.deleteAll(snapshotFiles)
    }

    override suspend fun setTaskCompleted(taskId: String, completed: Boolean) {
        database.withTransaction {
            val task = dao.getTask(taskId) ?: error("卡片不存在")
            if (!completed && task.spacedRepetitionEnabled) {
                val reviewFiles = mutableListOf<String>()
                ReviewSchedule.reviewSequences.forEach { sequence ->
                    val review = dao.findAutoReview(task.id, sequence)
                    if (review != null) {
                        reviewFiles += dao.getSnapshotsForTask(review.id)
                            .map(LearningSnapshotEntity::relativePath)
                    }
                }
                dao.deleteAutoReviews(task.id)
                snapshotStorage?.deleteAll(reviewFiles)
                dao.updateRepetitionState(
                    taskId = task.id,
                    enabled = false,
                    section = TaskSection.NEW.name,
                )
            }
            dao.updateTaskCompletion(taskId, completed)
        }
    }

    override suspend fun enableSpacedRepetition(sourceTaskId: String) {
        database.withTransaction {
            val source = dao.getTask(sourceTaskId) ?: error("卡片不存在")
            require(source.sourceType == TaskSourceType.ORIGINAL.name) {
                "只有新任务卡片专区的原始卡片可以生成复习安排"
            }
            require(source.isCompleted) { "请先完成卡片" }

            val sourceCards = dao.getCards(sourceTaskId)
            val now = nowProvider()
            ReviewSchedule.milestones(LocalDate.parse(source.createdDate)).forEach { milestone ->
                if (dao.findAutoReview(sourceTaskId, milestone.sequence) == null) {
                    val reviewTaskId = UUID.randomUUID().toString()
                    dao.insertTask(
                        StudyTaskEntity(
                            id = reviewTaskId,
                            title = source.title,
                            scheduledDate = milestone.date.toString(),
                            startTime = source.startTime,
                            section = TaskSection.REVIEW.name,
                            isCompleted = false,
                            spacedRepetitionEnabled = false,
                            createdDate = todayProvider().toString(),
                            createdAtEpochMillis = now + milestone.sequence,
                            sourceType = TaskSourceType.AUTO_REVIEW.name,
                            sourceTaskId = sourceTaskId,
                            reviewSequence = milestone.sequence,
                        ),
                    )
                    sourceCards.forEachIndexed { index, card ->
                        dao.insertCard(
                            card.copy(
                                id = UUID.randomUUID().toString(),
                                taskId = reviewTaskId,
                                createdAtEpochMillis = now + milestone.sequence * 100L + index,
                            ),
                        )
                    }
                }
            }

            dao.updateRepetitionState(
                taskId = sourceTaskId,
                enabled = true,
                section = TaskSection.REVIEW.name,
            )
        }
    }

    override suspend fun cancelSpacedRepetition(sourceTaskId: String) {
        database.withTransaction {
            val source = dao.getTask(sourceTaskId) ?: error("卡片不存在")
            require(source.sourceType == TaskSourceType.ORIGINAL.name) {
                "只有原始卡片可以关闭间隔复习"
            }
            cancelRepetitionInternal(
                source = source,
                targetSection = TaskSection.NEW,
            )
        }
    }

    override suspend fun addCard(
        taskId: String,
        objective: String,
        initialAction: String,
        acceptanceCriteria: String,
    ): String {
        validateCardFields(objective, initialAction, acceptanceCriteria)
        return database.withTransaction {
            val cardId = UUID.randomUUID().toString()
            dao.insertCard(
                TaskContentCardEntity(
                    id = cardId,
                    taskId = taskId,
                    objective = objective.trim(),
                    initialAction = initialAction.trim(),
                    acceptanceCriteria = acceptanceCriteria.trim(),
                    createdAtEpochMillis = nowProvider(),
                    sortOrder = dao.nextCardSortOrder(taskId),
                    acceptanceCompleted = false,
                    masteryLevel = null,
                ),
            )
            cardId
        }
    }

    override suspend fun updateCard(
        cardId: String,
        objective: String,
        initialAction: String,
        acceptanceCriteria: String,
    ) {
        validateCardFields(objective, initialAction, acceptanceCriteria)
        dao.updateCardFields(
            cardId = cardId,
            objective = objective.trim(),
            initialAction = initialAction.trim(),
            acceptanceCriteria = acceptanceCriteria.trim(),
        )
    }

    override suspend fun deleteCard(cardId: String) {
        val files = dao.getSnapshotsForCard(cardId)
            .map(LearningSnapshotEntity::relativePath)
        dao.deleteCard(cardId)
        snapshotStorage?.deleteAll(files)
    }

    override suspend fun setCardAcceptance(
        cardId: String,
        completed: Boolean,
        masteryLevel: Int?,
    ) {
        require(masteryLevel == null || masteryLevel in 0..4) { "掌握度无效" }
        require(completed || masteryLevel == null) { "取消完成时必须清除掌握度" }
        dao.updateCardAcceptance(
            cardId = cardId,
            completed = completed,
            masteryLevel = if (completed) masteryLevel else null,
        )
    }

    override suspend fun importSnapshot(
        cardId: String,
        sourceUri: Uri,
        kind: SnapshotKind,
    ): String {
        val storage = snapshotStorage ?: error("学习快照存储不可用")
        require(dao.getCard(cardId) != null) { "内容卡片不存在" }
        val saved = storage.save(cardId, sourceUri, kind)
        val snapshotId = UUID.randomUUID().toString()
        try {
            dao.insertSnapshot(
                LearningSnapshotEntity(
                    id = snapshotId,
                    cardId = cardId,
                    kind = kind.name,
                    displayName = saved.displayName,
                    relativePath = saved.relativePath,
                    mimeType = saved.mimeType,
                    createdAtEpochMillis = nowProvider(),
                ),
            )
            return snapshotId
        } catch (error: Exception) {
            storage.delete(saved.relativePath)
            throw error
        }
    }

    private suspend fun cancelRepetitionInternal(
        source: StudyTaskEntity,
        targetSection: TaskSection,
    ) {
        val reviewFiles = mutableListOf<String>()
        ReviewSchedule.reviewSequences.forEach { sequence ->
            val review = dao.findAutoReview(source.id, sequence)
            if (review != null) {
                reviewFiles += dao.getSnapshotsForTask(review.id)
                    .map(LearningSnapshotEntity::relativePath)
            }
        }
        dao.deleteAutoReviews(source.id)
        snapshotStorage?.deleteAll(reviewFiles)
        dao.updateRepetitionState(
            taskId = source.id,
            enabled = false,
            section = targetSection.name,
        )
    }

    private suspend fun seedTasks(today: LocalDate) {
        val now = nowProvider()
        val earlierDate = nearbyDate(today, -3)
        val laterDate = nearbyDate(today, 3)
        val mathTaskId = UUID.randomUUID().toString()
        val englishTaskId = UUID.randomUUID().toString()
        val readingTaskId = UUID.randomUUID().toString()
        val laterTaskId = UUID.randomUUID().toString()

        listOf(
            sampleTask(mathTaskId, today, LocalTime.of(9, 0), "数学错题整理", now, today),
            sampleTask(readingTaskId, today, LocalTime.of(19, 30), "阅读 20 分钟", now + 1, today),
            sampleTask(englishTaskId, earlierDate, LocalTime.of(16, 30), "英语单词复习", now + 2, today),
            sampleTask(laterTaskId, laterDate, LocalTime.of(18, 0), "英语单词复习", now + 3, today),
        ).forEach { dao.insertTask(it) }

        dao.insertCard(
            TaskContentCardEntity(
                id = UUID.randomUUID().toString(),
                taskId = mathTaskId,
                objective = "整理三道分数应用错题",
                initialAction = "翻开错题本，标出每道题出错的第一步",
                acceptanceCriteria = "不看答案，独立说清每道题的解题思路",
                createdAtEpochMillis = now + 10,
                sortOrder = 0,
                acceptanceCompleted = false,
                masteryLevel = null,
            ),
        )
    }

    private fun sampleTask(
        id: String,
        scheduledDate: LocalDate,
        startTime: LocalTime,
        title: String,
        createdAt: Long,
        today: LocalDate,
    ) = StudyTaskEntity(
        id = id,
        title = title,
        scheduledDate = scheduledDate.toString(),
        startTime = startTime.toString(),
        section = TaskSection.NEW.name,
        isCompleted = false,
        spacedRepetitionEnabled = false,
        createdDate = today.toString(),
        createdAtEpochMillis = createdAt,
        sourceType = TaskSourceType.ORIGINAL.name,
        sourceTaskId = null,
        reviewSequence = null,
    )

    private fun nearbyDate(today: LocalDate, dayOffset: Int): LocalDate {
        val targetDay = Math.floorMod(
            today.dayOfMonth - 1 + dayOffset,
            today.lengthOfMonth(),
        ) + 1
        return today.withDayOfMonth(targetDay)
    }

    private fun validateCardFields(
        objective: String,
        initialAction: String,
        acceptanceCriteria: String,
    ) {
        require(objective.isNotBlank()) { "卡片目标不能为空" }
        require(initialAction.isNotBlank()) { "初始动作不能为空" }
        require(acceptanceCriteria.isNotBlank()) { "验收标准不能为空" }
    }

    private companion object {
        const val SEED_METADATA_KEY = "v2_seed_complete"
    }
}

private fun StudyTaskEntity.toDomain() = StudyTask(
    id = id,
    title = title,
    scheduledDate = LocalDate.parse(scheduledDate),
    startTime = startTime?.let(LocalTime::parse),
    section = TaskSection.valueOf(section),
    isCompleted = isCompleted,
    spacedRepetitionEnabled = spacedRepetitionEnabled,
    createdDate = LocalDate.parse(createdDate),
    createdAtEpochMillis = createdAtEpochMillis,
    sourceType = TaskSourceType.valueOf(sourceType),
    sourceTaskId = sourceTaskId,
    reviewSequence = reviewSequence,
)

private fun TaskContentCardEntity.toDomain() = TaskContentCard(
    id = id,
    taskId = taskId,
    objective = objective,
    initialAction = initialAction,
    acceptanceCriteria = acceptanceCriteria,
    createdAtEpochMillis = createdAtEpochMillis,
    sortOrder = sortOrder,
    acceptanceCompleted = acceptanceCompleted,
    masteryLevel = masteryLevel,
)

private fun LearningSnapshotEntity.toDomain(localPath: String) = LearningSnapshot(
    id = id,
    cardId = cardId,
    kind = SnapshotKind.valueOf(kind),
    displayName = displayName,
    localPath = localPath,
    createdAtEpochMillis = createdAtEpochMillis,
)
