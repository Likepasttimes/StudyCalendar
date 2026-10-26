package com.learningcalendar.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTaskDao {
    @Query(
        """
        SELECT scheduledDate AS date, COUNT(*) AS taskCount
        FROM study_tasks
        WHERE scheduledDate BETWEEN :startDate AND :endDate
        GROUP BY scheduledDate
        """,
    )
    fun observeTaskCounts(startDate: String, endDate: String): Flow<List<TaskCountRow>>

    @Query(
        """
        SELECT * FROM study_tasks
        WHERE scheduledDate = :date
        ORDER BY
            CASE WHEN startTime IS NULL THEN 1 ELSE 0 END,
            startTime ASC,
            createdAtEpochMillis ASC,
            id ASC
        """,
    )
    fun observeTasksForDate(date: String): Flow<List<StudyTaskEntity>>

    @Query(
        """
        SELECT * FROM study_tasks
        WHERE scheduledDate BETWEEN :startDate AND :endDate
        ORDER BY scheduledDate ASC,
            CASE WHEN startTime IS NULL THEN 1 ELSE 0 END,
            startTime ASC,
            createdAtEpochMillis ASC,
            id ASC
        """,
    )
    fun observeTasksBetween(
        startDate: String,
        endDate: String,
    ): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks WHERE id = :taskId LIMIT 1")
    fun observeTask(taskId: String): Flow<StudyTaskEntity?>

    @Query("SELECT * FROM study_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTask(taskId: String): StudyTaskEntity?

    @Query("SELECT * FROM study_tasks ORDER BY scheduledDate, createdAtEpochMillis, id")
    fun observeAllTasks(): Flow<List<StudyTaskEntity>>

    @Query("SELECT * FROM study_tasks ORDER BY scheduledDate, createdAtEpochMillis, id")
    suspend fun getAllTasksForSync(): List<StudyTaskEntity>

    @Query("SELECT COUNT(*) FROM study_tasks")
    suspend fun taskCount(): Int

    @Query(
        """
        SELECT * FROM study_tasks
        WHERE sourceTaskId = :sourceTaskId AND reviewSequence = :sequence
          AND sourceType = 'AUTO_REVIEW'
        LIMIT 1
        """,
    )
    suspend fun findAutoReview(sourceTaskId: String, sequence: Int): StudyTaskEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTask(task: StudyTaskEntity)

    @Query(
        """
        UPDATE study_tasks
        SET title = :title, startTime = :startTime
        WHERE id = :taskId
        """,
    )
    suspend fun updateTaskFields(taskId: String, title: String, startTime: String?)

    @Query("UPDATE study_tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, completed: Boolean)

    @Query(
        """
        UPDATE study_tasks
        SET spacedRepetitionEnabled = :enabled, section = :section
        WHERE id = :taskId
        """,
    )
    suspend fun updateRepetitionState(
        taskId: String,
        enabled: Boolean,
        section: String,
    )

    @Query("DELETE FROM study_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query(
        """
        DELETE FROM study_tasks
        WHERE sourceTaskId = :sourceTaskId AND sourceType = 'AUTO_REVIEW'
        """,
    )
    suspend fun deleteAutoReviews(sourceTaskId: String)

    @Query(
        """
        SELECT * FROM task_content_cards
        WHERE taskId = :taskId
        ORDER BY sortOrder ASC, createdAtEpochMillis ASC, id ASC
        """,
    )
    fun observeCards(taskId: String): Flow<List<TaskContentCardEntity>>

    @Query(
        """
        SELECT * FROM task_content_cards
        WHERE taskId = :taskId
        ORDER BY sortOrder ASC, createdAtEpochMillis ASC, id ASC
        """,
    )
    suspend fun getCards(taskId: String): List<TaskContentCardEntity>

    @Query(
        """
        SELECT * FROM task_content_cards
        ORDER BY taskId, sortOrder ASC, createdAtEpochMillis ASC, id ASC
        """,
    )
    suspend fun getAllCardsForSync(): List<TaskContentCardEntity>

    @Query("SELECT * FROM task_content_cards WHERE id = :cardId LIMIT 1")
    fun observeCard(cardId: String): Flow<TaskContentCardEntity?>

    @Query("SELECT * FROM task_content_cards WHERE id = :cardId LIMIT 1")
    suspend fun getCard(cardId: String): TaskContentCardEntity?

    @Query(
        """
        SELECT COALESCE(MAX(sortOrder), -1) + 1
        FROM task_content_cards
        WHERE taskId = :taskId
        """,
    )
    suspend fun nextCardSortOrder(taskId: String): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCard(card: TaskContentCardEntity)

    @Query(
        """
        UPDATE task_content_cards
        SET objective = :objective,
            initialAction = :initialAction,
            acceptanceCriteria = :acceptanceCriteria
        WHERE id = :cardId
        """,
    )
    suspend fun updateCardFields(
        cardId: String,
        objective: String,
        initialAction: String,
        acceptanceCriteria: String,
    )

    @Query(
        """
        UPDATE task_content_cards
        SET acceptanceCompleted = :completed, masteryLevel = :masteryLevel
        WHERE id = :cardId
        """,
    )
    suspend fun updateCardAcceptance(
        cardId: String,
        completed: Boolean,
        masteryLevel: Int?,
    )

    @Query("DELETE FROM task_content_cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: String)

    @Query(
        """
        SELECT * FROM learning_snapshots
        WHERE cardId = :cardId
        ORDER BY createdAtEpochMillis ASC, id ASC
        """,
    )
    fun observeSnapshots(cardId: String): Flow<List<LearningSnapshotEntity>>

    @Query("SELECT * FROM learning_snapshots WHERE cardId = :cardId")
    suspend fun getSnapshotsForCard(cardId: String): List<LearningSnapshotEntity>

    @Query(
        """
        SELECT learning_snapshots.* FROM learning_snapshots
        INNER JOIN task_content_cards ON task_content_cards.id = learning_snapshots.cardId
        WHERE task_content_cards.taskId = :taskId
        """,
    )
    suspend fun getSnapshotsForTask(taskId: String): List<LearningSnapshotEntity>

    @Query(
        """
        SELECT * FROM learning_snapshots
        ORDER BY cardId, createdAtEpochMillis ASC, id ASC
        """,
    )
    suspend fun getAllSnapshotsForSync(): List<LearningSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(snapshot: LearningSnapshotEntity)

    @Query("DELETE FROM learning_snapshots WHERE id = :snapshotId")
    suspend fun deleteSnapshot(snapshotId: String)

    @Query("SELECT * FROM learning_snapshots WHERE id = :snapshotId LIMIT 1")
    suspend fun getLearningSnapshot(snapshotId: String): LearningSnapshotEntity?

    @Query(
        """
        SELECT * FROM error_records
        WHERE cardId = :cardId
        ORDER BY createdAtEpochMillis ASC, id ASC
        """,
    )
    fun observeErrorRecords(cardId: String): Flow<List<ErrorRecordEntity>>

    @Query(
        """
        SELECT * FROM error_records
        ORDER BY cardId, createdAtEpochMillis ASC, id ASC
        """,
    )
    suspend fun getAllErrorRecordsForSync(): List<ErrorRecordEntity>

    @Query("SELECT * FROM error_records WHERE id = :recordId LIMIT 1")
    suspend fun getErrorRecord(recordId: String): ErrorRecordEntity?

    @Query(
        """
        SELECT error_screenshots.* FROM error_screenshots
        INNER JOIN error_records ON error_records.id = error_screenshots.errorRecordId
        WHERE error_records.cardId = :cardId
        ORDER BY error_screenshots.createdAtEpochMillis ASC, error_screenshots.id ASC
        """,
    )
    fun observeErrorScreenshotsForCard(cardId: String): Flow<List<ErrorScreenshotEntity>>

    @Query("SELECT * FROM error_screenshots WHERE errorRecordId = :recordId")
    suspend fun getErrorScreenshotsForRecord(recordId: String): List<ErrorScreenshotEntity>

    @Query(
        """
        SELECT error_screenshots.* FROM error_screenshots
        INNER JOIN error_records ON error_records.id = error_screenshots.errorRecordId
        WHERE error_records.cardId = :cardId
        """,
    )
    suspend fun getErrorScreenshotsForCard(cardId: String): List<ErrorScreenshotEntity>

    @Query(
        """
        SELECT error_screenshots.* FROM error_screenshots
        INNER JOIN error_records ON error_records.id = error_screenshots.errorRecordId
        INNER JOIN task_content_cards ON task_content_cards.id = error_records.cardId
        WHERE task_content_cards.taskId = :taskId
        """,
    )
    suspend fun getErrorScreenshotsForTask(taskId: String): List<ErrorScreenshotEntity>

    @Query(
        """
        SELECT * FROM error_screenshots
        ORDER BY errorRecordId, createdAtEpochMillis ASC, id ASC
        """,
    )
    suspend fun getAllErrorScreenshotsForSync(): List<ErrorScreenshotEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertErrorRecord(record: ErrorRecordEntity)

    @Query(
        """
        UPDATE error_records
        SET errorPoint = :errorPoint,
            description = :description,
            updatedAtEpochMillis = :updatedAt
        WHERE id = :recordId
        """,
    )
    suspend fun updateErrorRecord(
        recordId: String,
        errorPoint: String,
        description: String,
        updatedAt: Long,
    )

    @Query("DELETE FROM error_records WHERE id = :recordId")
    suspend fun deleteErrorRecord(recordId: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertErrorScreenshot(screenshot: ErrorScreenshotEntity)

    @Query("DELETE FROM error_screenshots WHERE id = :screenshotId")
    suspend fun deleteErrorScreenshot(screenshotId: String)

    @Query("SELECT * FROM error_screenshots WHERE id = :screenshotId LIMIT 1")
    suspend fun getErrorScreenshot(screenshotId: String): ErrorScreenshotEntity?

    @Query(
        """
        SELECT * FROM week_summary_documents
        WHERE weekStartDate = :weekStartDate
        ORDER BY uploadedAtEpochMillis ASC, id ASC
        """,
    )
    fun observeWeekSummaryDocuments(weekStartDate: String): Flow<List<WeekSummaryDocumentEntity>>

    @Query(
        """
        SELECT * FROM week_summary_documents
        WHERE uploadedAtEpochMillis BETWEEN :startMillis AND :endMillis
        ORDER BY uploadedAtEpochMillis DESC, id ASC
        """,
    )
    fun observeWeekSummaryDocumentsUploadedBetween(
        startMillis: Long,
        endMillis: Long,
    ): Flow<List<WeekSummaryDocumentEntity>>

    @Query(
        """
        SELECT * FROM week_summary_documents
        WHERE uploadedAtEpochMillis BETWEEN :startTimestamp AND :endTimestamp
        ORDER BY uploadedAtEpochMillis ASC, id ASC
        """,
    )
    fun observeWeekSummaryDocumentsBetween(
        startTimestamp: Long,
        endTimestamp: Long,
    ): Flow<List<WeekSummaryDocumentEntity>>

    @Query(
        """
        SELECT * FROM week_summary_documents
        ORDER BY weekStartDate ASC, uploadedAtEpochMillis ASC, id ASC
        """,
    )
    suspend fun getAllWeekSummaryDocumentsForSync(): List<WeekSummaryDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWeekSummaryDocument(document: WeekSummaryDocumentEntity)

    @Query("SELECT * FROM week_summary_documents WHERE id = :documentId LIMIT 1")
    suspend fun getWeekSummaryDocument(documentId: String): WeekSummaryDocumentEntity?

    @Query("DELETE FROM week_summary_documents WHERE id = :documentId")
    suspend fun deleteWeekSummaryDocument(documentId: String)

    @Query("SELECT value FROM app_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMetadata(metadata: AppMetadataEntity)
}
