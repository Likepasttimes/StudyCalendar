package com.learningcalendar.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.learningcalendar.app.data.local.LearningCalendarDatabase
import com.learningcalendar.app.data.local.LocalSnapshotStorage
import com.learningcalendar.app.domain.MasteryLevel
import com.learningcalendar.app.domain.SnapshotKind
import com.learningcalendar.app.domain.TaskSection
import com.learningcalendar.app.domain.TaskSourceType
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomStudyTaskRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: LearningCalendarDatabase
    private lateinit var repository: RoomStudyTaskRepository
    private var clock = 10_000L
    private val today = LocalDate.of(2026, 10, 25)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            LearningCalendarDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = RoomStudyTaskRepository(
            database = database,
            snapshotStorage = LocalSnapshotStorage(context),
            todayProvider = { today },
            nowProvider = { clock++ },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun enablingCreatesExactlyThreeIdempotentSnapshots() = runTest {
        val sourceId = repository.createTask(
            title = "分数应用题复习",
            scheduledDate = today,
            startTime = LocalTime.of(9, 0),
            section = TaskSection.NEW,
        )
        repository.addCard(
            sourceId,
            objective = "理解单位一",
            initialAction = "圈出单位一",
            acceptanceCriteria = "完成两道变式题",
        )
        repository.setTaskCompleted(sourceId, true)

        repository.enableSpacedRepetition(sourceId)
        repository.enableSpacedRepetition(sourceId)

        val allTasks = database.studyTaskDao().observeAllTasks().first()
        val reviews = allTasks.filter { it.sourceTaskId == sourceId }
        val source = allTasks.single { it.id == sourceId }
        assertEquals(3, reviews.size)
        assertEquals(listOf(2, 3, 7), reviews.mapNotNull { it.reviewSequence }.sorted())
        assertEquals(
            listOf(today.plusDays(1), today.plusDays(2), today.plusDays(6)),
            reviews.map { LocalDate.parse(it.scheduledDate) }.sorted(),
        )
        assertTrue(reviews.all { it.sourceType == TaskSourceType.AUTO_REVIEW.name })
        assertTrue(reviews.all { it.section == TaskSection.REVIEW.name })
        assertEquals(TaskSection.REVIEW.name, source.section)
        assertTrue(source.spacedRepetitionEnabled)
        reviews.forEach { review ->
            val cards = repository.observeCards(review.id).first()
            assertEquals(1, cards.size)
            assertEquals("理解单位一", cards.single().objective)
        }

        val firstReviewCards = repository.observeCards(reviews.first().id).first()
        val secondReviewCards = repository.observeCards(reviews[1].id).first()
        repository.updateCard(
            firstReviewCards.single().id,
            "只修改第 ${reviews.first().reviewSequence} 天",
            "独立动作",
            "独立标准",
        )
        assertEquals(
            "理解单位一",
            repository.observeCards(secondReviewCards.single().taskId).first().single().objective,
        )
    }

    @Test
    fun cancelRemovesOnlyAutoReviewsAndReturnsOriginalToNewSection() = runTest {
        val sourceId = completedOriginal()
        repository.enableSpacedRepetition(sourceId)

        repository.cancelSpacedRepetition(sourceId)

        val tasks = database.studyTaskDao().observeAllTasks().first()
        assertTrue(tasks.none { it.sourceTaskId == sourceId })
        val source = tasks.single { it.id == sourceId }
        assertEquals(TaskSection.NEW.name, source.section)
        assertFalse(source.spacedRepetitionEnabled)
        assertTrue(source.isCompleted)
    }

    @Test
    fun completingTaskDoesNotChangeItsStoredSection() = runTest {
        val taskId = repository.createTask(
            "自动归档",
            today,
            LocalTime.of(10, 0),
            TaskSection.NEW,
        )

        repository.setTaskCompleted(taskId, true)
        assertEquals(TaskSection.NEW.name, database.studyTaskDao().getTask(taskId)?.section)

        repository.setTaskCompleted(taskId, false)
        assertEquals(TaskSection.NEW.name, database.studyTaskDao().getTask(taskId)?.section)
    }

    @Test
    fun uncompletingRepeatingSourceRemovesChainButKeepsSource() = runTest {
        val sourceId = completedOriginal()
        repository.enableSpacedRepetition(sourceId)

        repository.setTaskCompleted(sourceId, false)

        val tasks = database.studyTaskDao().observeAllTasks().first()
        val source = tasks.single { it.id == sourceId }
        assertFalse(source.isCompleted)
        assertFalse(source.spacedRepetitionEnabled)
        assertEquals(TaskSection.NEW.name, source.section)
        assertEquals(1, tasks.size)
    }

    @Test
    fun deletingOriginalCascadesAutoReviewsAndTheirCards() = runTest {
        val sourceId = completedOriginal()
        repository.addCard(sourceId, "目标", "动作", "标准")
        repository.enableSpacedRepetition(sourceId)
        val reviewIds = database.studyTaskDao()
            .observeAllTasks()
            .first()
            .filter { it.sourceTaskId == sourceId }
            .map { it.id }

        repository.deleteTask(sourceId)

        val tasks = database.studyTaskDao().observeAllTasks().first()
        assertTrue(tasks.isEmpty())
        reviewIds.forEach { id ->
            assertTrue(repository.observeCards(id).first().isEmpty())
        }
    }

    @Test
    fun autoReviewCannotGenerateAnotherReviewChain() = runTest {
        val sourceId = completedOriginal()
        repository.enableSpacedRepetition(sourceId)
        val review = database.studyTaskDao().observeAllTasks().first()
            .first { it.sourceTaskId == sourceId }

        repository.setTaskCompleted(review.id, true)
        val result = runCatching {
            repository.enableSpacedRepetition(review.id)
        }

        assertTrue(result.isFailure)
        assertEquals(
            3,
            database.studyTaskDao().observeAllTasks().first().count { it.sourceTaskId == sourceId },
        )
    }

    @Test
    fun deletingOneAutoReviewDoesNotDeleteSourceOrOtherInstances() = runTest {
        val sourceId = completedOriginal()
        repository.enableSpacedRepetition(sourceId)
        val review = database.studyTaskDao().observeAllTasks().first()
            .first { it.sourceTaskId == sourceId }

        repository.deleteTask(review.id)

        val remaining = database.studyTaskDao().observeAllTasks().first()
        assertTrue(remaining.any { it.id == sourceId })
        assertEquals(2, remaining.count { it.sourceTaskId == sourceId })
    }

    @Test
    fun taskQuerySortsByTimeThenCreationAndPlacesMissingTimeLast() = runTest {
        val noTime = repository.createTask("无时间", today, null, TaskSection.NEW)
        val later = repository.createTask(
            "晚一些",
            today,
            LocalTime.of(16, 0),
            TaskSection.NEW,
        )
        val earlier = repository.createTask(
            "早一些",
            today,
            LocalTime.of(8, 0),
            TaskSection.NEW,
        )

        val ordered = repository.observeTasksForDate(today).first()

        assertEquals(listOf(earlier, later, noTime), ordered.map { it.id })
    }

    @Test
    fun cardOrderIsStableAfterEditing() = runTest {
        val taskId = repository.createTask("卡片排序", today, null, TaskSection.NEW)
        val first = repository.addCard(taskId, "目标一", "动作一", "标准一")
        val second = repository.addCard(taskId, "目标二", "动作二", "标准二")
        val third = repository.addCard(taskId, "目标三", "动作三", "标准三")

        repository.updateCard(second, "修改后的目标", "修改后的动作", "修改后的标准")
        val cards = repository.observeCards(taskId).first()

        assertEquals(listOf(first, second, third), cards.map { it.id })
        assertEquals(listOf(0L, 1L, 2L), cards.map { it.sortOrder })
        assertEquals("修改后的目标", cards[1].objective)
    }

    @Test
    fun masteryZeroPersistsAndUncheckingClearsMastery() = runTest {
        val taskId = repository.createTask("验收测试", today, null, TaskSection.NEW)
        val cardId = repository.addCard(taskId, "目标", "动作", "标准")

        repository.setCardAcceptance(cardId, true, null)
        assertEquals(null, repository.observeCard(cardId).first()?.masteryLevel)

        repository.setCardAcceptance(cardId, true, MasteryLevel.VIEWED.value)
        val stored = repository.observeCard(cardId).first()
        assertTrue(stored?.acceptanceCompleted == true)
        assertEquals(0, stored?.masteryLevel)

        repository.setCardAcceptance(cardId, false, null)
        val uncompleted = repository.observeCard(cardId).first()
        assertFalse(uncompleted?.acceptanceCompleted == true)
        assertNull(uncompleted?.masteryLevel)

        repository.deleteCard(cardId)
        assertNull(repository.observeCard(cardId).first())
    }

    @Test
    fun masteryZeroSurvivesDatabaseReopen() = runTest {
        val databaseName = "persistence-${UUID.randomUUID()}.db"
        context.deleteDatabase(databaseName)
        val firstDatabase = Room.databaseBuilder(
            context,
            LearningCalendarDatabase::class.java,
            databaseName,
        ).allowMainThreadQueries().build()
        val firstRepository = RoomStudyTaskRepository(
            firstDatabase,
            todayProvider = { today },
            nowProvider = { clock++ },
        )
        val taskId = firstRepository.createTask("持久化", today, null, TaskSection.NEW)
        val cardId = firstRepository.addCard(taskId, "目标", "动作", "标准")
        firstRepository.setCardAcceptance(cardId, true, 0)
        firstDatabase.close()

        val reopenedDatabase = Room.databaseBuilder(
            context,
            LearningCalendarDatabase::class.java,
            databaseName,
        ).allowMainThreadQueries().build()
        val reopenedRepository = RoomStudyTaskRepository(
            reopenedDatabase,
            todayProvider = { today },
            nowProvider = { clock++ },
        )
        val restored = reopenedRepository.observeCard(cardId).first()

        assertTrue(restored?.acceptanceCompleted == true)
        assertEquals(0, restored?.masteryLevel)

        reopenedDatabase.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun sampleSeedRunsOnlyOnceEvenAfterAllTasksAreDeleted() = runTest {
        repository.seedIfNeeded()
        val seeded = database.studyTaskDao().observeAllTasks().first()
        assertTrue(seeded.isNotEmpty())
        seeded.forEach { database.studyTaskDao().deleteTask(it.id) }

        repository.seedIfNeeded()

        assertTrue(database.studyTaskDao().observeAllTasks().first().isEmpty())
    }

    @Test
    fun learningSnapshotIsCopiedToAppFolderAndRemovedWithCard() = runTest {
        val taskId = repository.createTask("快照卡片", today, null, TaskSection.NEW)
        val cardId = repository.addCard(taskId, "目标", "动作", "标准")
        val sourceFile = File(context.cacheDir, "source-photo.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3, 4, 5))

        repository.importSnapshot(
            cardId = cardId,
            sourceUri = android.net.Uri.fromFile(sourceFile),
            kind = SnapshotKind.PHOTO,
        )
        val snapshot = repository.observeSnapshots(cardId).first().single()
        val copiedFile = File(snapshot.localPath)

        assertTrue(snapshot.localPath.contains("学习快照"))
        assertTrue(copiedFile.exists())
        assertEquals(5, copiedFile.length())

        repository.deleteCard(cardId)

        assertTrue(snapshot.localPath.isNotEmpty())
        assertFalse(File(snapshot.localPath).exists())
        sourceFile.delete()
    }

    @Test
    fun learningSnapshotCanBeDeletedIndividually() = runTest {
        val taskId = repository.createTask("快照删除", today, null, TaskSection.NEW)
        val cardId = repository.addCard(taskId, "目标", "动作", "标准")
        val sourceFile = File(context.cacheDir, "delete-snapshot.jpg")
        sourceFile.writeBytes(byteArrayOf(1, 2, 3))

        val snapshotId = repository.importSnapshot(
            cardId,
            android.net.Uri.fromFile(sourceFile),
            SnapshotKind.PHOTO,
        )
        val snapshot = repository.observeSnapshots(cardId).first().single()

        repository.deleteLearningSnapshot(snapshotId)

        assertTrue(repository.observeSnapshots(cardId).first().isEmpty())
        assertFalse(File(snapshot.localPath).exists())
        sourceFile.delete()
    }

    @Test
    fun errorRecordSupportsCrudAndScreenshotCleanup() = runTest {
        val taskId = repository.createTask("错误记录", today, null, TaskSection.NEW)
        val cardId = repository.addCard(taskId, "目标", "动作", "标准")
        val recordId = repository.createErrorRecord(cardId, "单位一判断错误", "把总人数当成单位一")

        var records = repository.observeErrorRecords(cardId).first()
        assertEquals(1, records.size)
        assertEquals("单位一判断错误", records.single().errorPoint)

        repository.updateErrorRecord(recordId, "单位一选错", "没有先确认整体数量")
        records = repository.observeErrorRecords(cardId).first()
        assertEquals("单位一选错", records.single().errorPoint)
        assertEquals("没有先确认整体数量", records.single().description)

        val sourceFile = File(context.cacheDir, "error-shot.jpg")
        sourceFile.writeBytes(byteArrayOf(9, 8, 7))
        repository.importErrorScreenshot(recordId, android.net.Uri.fromFile(sourceFile))
        val screenshot = repository.observeErrorScreenshotsForCard(cardId).first().single()
        assertTrue(File(screenshot.localPath).exists())

        repository.deleteErrorScreenshot(screenshot.id)
        assertTrue(repository.observeErrorScreenshotsForCard(cardId).first().isEmpty())
        assertFalse(File(screenshot.localPath).exists())

        val replacementFile = File(context.cacheDir, "error-shot-2.jpg")
        replacementFile.writeBytes(byteArrayOf(6, 5, 4))
        repository.importErrorScreenshot(recordId, android.net.Uri.fromFile(replacementFile))
        val replacement = repository.observeErrorScreenshotsForCard(cardId).first().single()

        repository.deleteErrorRecord(recordId)

        assertTrue(repository.observeErrorRecords(cardId).first().isEmpty())
        assertFalse(File(replacement.localPath).exists())
        sourceFile.delete()
        replacementFile.delete()
    }

    @Test
    fun errorRecordAllowsBlankDescription() = runTest {
        val taskId = repository.createTask("选填描述", today, null, TaskSection.NEW)
        val cardId = repository.addCard(taskId, "目标", "动作", "标准")

        val recordId = repository.createErrorRecord(cardId, "单位遗漏", "   ")
        val created = repository.observeErrorRecords(cardId).first().single()
        assertEquals("", created.description)

        repository.updateErrorRecord(recordId, "单位遗漏", "")
        val updated = repository.observeErrorRecords(cardId).first().single()
        assertEquals("", updated.description)
    }

    @Test
    fun errorRecordCreationImportsDraftPhotoAndFileAttachments() = runTest {
        val taskId = repository.createTask("错误附件", today, null, TaskSection.NEW)
        val cardId = repository.addCard(taskId, "目标", "动作", "标准")
        val photoFile = File(context.cacheDir, "draft-photo.jpg")
        val documentFile = File(context.cacheDir, "draft-error.txt")
        photoFile.writeBytes(byteArrayOf(1, 2, 3))
        documentFile.writeBytes(byteArrayOf(4, 5, 6))

        val recordId = repository.createErrorRecordWithAttachments(
            cardId = cardId,
            errorPoint = "步骤遗漏",
            description = "忘记写单位",
            photoUris = listOf(android.net.Uri.fromFile(photoFile)),
            fileUris = listOf(android.net.Uri.fromFile(documentFile)),
        )

        val attachments = repository.observeErrorScreenshotsForCard(cardId).first()
        assertEquals(2, attachments.size)
        assertTrue(attachments.all { File(it.localPath).exists() })
        assertEquals(1, repository.observeErrorRecords(cardId).first().size)

        repository.deleteErrorRecord(recordId)
        assertTrue(attachments.none { File(it.localPath).exists() })
        photoFile.delete()
        documentFile.delete()
    }

    @Test
    fun weeklySummaryDocumentCanBeUploadedAndDeleted() = runTest {
        val weekStart = LocalDate.of(2026, 10, 26)
        val sourceFile = File(context.cacheDir, "weekly-summary.docx")
        sourceFile.writeBytes(byteArrayOf(4, 3, 2, 1))

        val documentId = repository.importWeekSummaryDocument(
            weekStart,
            android.net.Uri.fromFile(sourceFile),
        )
        val document = repository.observeWeekSummaryDocuments(weekStart).first().single()
        assertTrue(document.displayName.endsWith(".docx"))
        assertTrue(File(document.localPath).exists())
        assertEquals(
            1,
            repository.observeWeekSummaryDocumentsForMonth(
                YearMonth.from(
                    Instant.ofEpochMilli(document.uploadedAtEpochMillis)
                        .atZone(ZoneId.systemDefault()),
                ),
            ).first().size,
        )

        repository.deleteWeekSummaryDocument(documentId)

        assertTrue(repository.observeWeekSummaryDocuments(weekStart).first().isEmpty())
        assertFalse(File(document.localPath).exists())
        sourceFile.delete()
    }

    private suspend fun completedOriginal(): String {
        val id = repository.createTask(
            title = "原始任务",
            scheduledDate = today,
            startTime = LocalTime.of(8, 30),
            section = TaskSection.NEW,
        )
        repository.setTaskCompleted(id, true)
        return id
    }
}
