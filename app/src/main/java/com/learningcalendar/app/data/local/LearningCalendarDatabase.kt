package com.learningcalendar.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        StudyTaskEntity::class,
        TaskContentCardEntity::class,
        LearningSnapshotEntity::class,
        ErrorRecordEntity::class,
        ErrorScreenshotEntity::class,
        WeekSummaryDocumentEntity::class,
        AppMetadataEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class LearningCalendarDatabase : RoomDatabase() {
    abstract fun studyTaskDao(): StudyTaskDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS learning_snapshots (
                        id TEXT NOT NULL PRIMARY KEY,
                        cardId TEXT NOT NULL,
                        kind TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        relativePath TEXT NOT NULL,
                        mimeType TEXT,
                        createdAtEpochMillis INTEGER NOT NULL,
                        FOREIGN KEY(cardId) REFERENCES task_content_cards(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_learning_snapshots_cardId " +
                        "ON learning_snapshots(cardId)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS error_records (
                        id TEXT NOT NULL PRIMARY KEY,
                        cardId TEXT NOT NULL,
                        errorPoint TEXT NOT NULL,
                        description TEXT NOT NULL,
                        createdAtEpochMillis INTEGER NOT NULL,
                        updatedAtEpochMillis INTEGER NOT NULL,
                        FOREIGN KEY(cardId) REFERENCES task_content_cards(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_error_records_cardId " +
                        "ON error_records(cardId)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS error_screenshots (
                        id TEXT NOT NULL PRIMARY KEY,
                        errorRecordId TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        relativePath TEXT NOT NULL,
                        mimeType TEXT,
                        createdAtEpochMillis INTEGER NOT NULL,
                        FOREIGN KEY(errorRecordId) REFERENCES error_records(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_error_screenshots_errorRecordId " +
                        "ON error_screenshots(errorRecordId)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS week_summary_documents (
                        id TEXT NOT NULL PRIMARY KEY,
                        weekStartDate TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        relativePath TEXT NOT NULL,
                        mimeType TEXT,
                        uploadedAtEpochMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_week_summary_documents_weekStartDate " +
                        "ON week_summary_documents(weekStartDate)",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE study_tasks ADD COLUMN repetitionStartDate TEXT",
                )
            }
        }
    }
}
