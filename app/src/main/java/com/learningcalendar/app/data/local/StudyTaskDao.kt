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

    @Query("SELECT * FROM study_tasks WHERE id = :taskId LIMIT 1")
    fun observeTask(taskId: String): Flow<StudyTaskEntity?>

    @Query("SELECT * FROM study_tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTask(taskId: String): StudyTaskEntity?

    @Query("SELECT * FROM study_tasks ORDER BY scheduledDate, createdAtEpochMillis, id")
    fun observeAllTasks(): Flow<List<StudyTaskEntity>>

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

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(snapshot: LearningSnapshotEntity)

    @Query("DELETE FROM learning_snapshots WHERE id = :snapshotId")
    suspend fun deleteSnapshot(snapshotId: String)

    @Query("SELECT value FROM app_metadata WHERE `key` = :key LIMIT 1")
    suspend fun getMetadata(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putMetadata(metadata: AppMetadataEntity)
}
