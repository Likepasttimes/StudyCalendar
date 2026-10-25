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
        AppMetadataEntity::class,
    ],
    version = 2,
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
    }
}
