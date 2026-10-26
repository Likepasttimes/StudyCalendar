package com.learningcalendar.desktop.sync

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.json.JSONObject

class DesktopSyncTest {
    @Test
    fun serverAcceptsValidPairingCodeAndInstallsSnapshot() {
        val root = Files.createTempDirectory("learning-calendar-sync-test").toFile()
        val store = DesktopSyncStore(root)
        val server = DesktopSyncServer(store, preferredPort = 0).start()
        try {
            val zip = createBundle(root)
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:${server.port}/api/v1/sync"))
                .header("Content-Type", "application/zip")
                .header("X-Pairing-Code", server.pairingCode)
                .header("X-Format-Version", "1")
                .POST(HttpRequest.BodyPublishers.ofFile(zip.toPath()))
                .build()

            val response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString(),
            )

            assertEquals(200, response.statusCode())
            assertTrue(response.body().contains("\"ok\":true"))
            assertEquals("数学复习", store.current?.snapshot?.tasks?.single()?.title)

            val token = JSONObject(response.body()).getString("syncToken")
            assertTrue(token.matches(Regex("[0-9a-f]{64}")))
            val tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:${server.port}/api/v1/sync"))
                .header("Content-Type", "application/zip")
                .header("X-Sync-Token", token)
                .header("X-Format-Version", "1")
                .POST(HttpRequest.BodyPublishers.ofFile(zip.toPath()))
                .build()
            val tokenResponse = HttpClient.newHttpClient().send(
                tokenRequest,
                HttpResponse.BodyHandlers.ofString(),
            )
            assertEquals(200, tokenResponse.statusCode())
        } finally {
            server.close()
            root.deleteRecursively()
        }
    }

    @Test
    fun serverRejectsIncorrectPairingCode() {
        val root = Files.createTempDirectory("learning-calendar-sync-auth-test").toFile()
        val store = DesktopSyncStore(root)
        val server = DesktopSyncServer(store, preferredPort = 0).start()
        try {
            val zip = createBundle(root)
            val incorrectCode = if (server.pairingCode == "000000") "000001" else "000000"
            val request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:${server.port}/api/v1/sync"))
                .header("Content-Type", "application/zip")
                .header("X-Pairing-Code", incorrectCode)
                .header("X-Format-Version", "1")
                .POST(HttpRequest.BodyPublishers.ofFile(zip.toPath()))
                .build()

            val response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString(),
            )

            assertEquals(401, response.statusCode())
            assertEquals(null, store.current)
        } finally {
            server.close()
            root.deleteRecursively()
        }
    }

    @Test
    fun configuredDataDirectoryMigrationCopiesSnapshotsAndRewritesPointer() {
        val root = Files.createTempDirectory("learning-calendar-data-location-test").toFile()
        try {
            val legacy = File(root, "legacy")
            val selected = File(root, "selected")
            val legacySnapshot = File(legacy, "snapshots/snapshot-1/files")
            legacySnapshot.mkdirs()
            File(legacySnapshot, "sample.txt").writeText("保留数据", Charsets.UTF_8)
            File(legacy, "current-snapshot.txt").writeText(
                File(legacy, "snapshots/snapshot-1").absolutePath,
                Charsets.UTF_8,
            )
            selected.mkdirs()

            val configured = DesktopDataLocation.parseConfigDirectory(
                "data.root=${selected.absolutePath}",
            )
            assertEquals(selected.absoluteFile.normalize(), configured?.absoluteFile?.normalize())
            assertEquals(
                File("D:\\学习日历数据"),
                DesktopDataLocation.parseConfigDirectory(
                    """data.root=D:\\u5b66\u4e60\u65e5\u5386\u6570\u636e""",
                ),
            )

            DesktopDataLocation.migrateLegacyDataIfNeeded(legacy, selected)

            assertTrue(
                File(selected, "snapshots/snapshot-1/files/sample.txt").isFile,
            )
            assertEquals(
                File(selected, "snapshots/snapshot-1").absolutePath,
                File(selected, "current-snapshot.txt").readText(Charsets.UTF_8),
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun weeklySummaryDocumentCanBeUploadedAndDeleted() {
        val root = Files.createTempDirectory("learning-calendar-weekly-summary-test").toFile()
        try {
            val store = DesktopSyncStore(root)
            store.install(createBundle(root))
            val source = File(root, "周总结.docx")
            source.writeBytes(byteArrayOf(1, 2, 3, 4))
            val weekStart = LocalDate.of(2026, 10, 26)

            val uploaded = store.importWeekSummaryDocument(weekStart, source)
            val document = uploaded.snapshot.weekDocuments.single()
            assertEquals("周总结.docx", document.displayName)
            assertTrue(store.resolveAttachment(document.relativePath)?.isFile == true)

            store.install(createBundle(root))
            assertEquals(document.id, store.current?.snapshot?.weekDocuments?.single()?.id)
            assertTrue(store.resolveAttachment(document.relativePath)?.isFile == true)

            val deleted = store.deleteWeekSummaryDocument(document.id)
            assertTrue(deleted.snapshot.weekDocuments.isEmpty())
            assertTrue(store.resolveAttachment(document.relativePath) == null)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun createBundle(directory: File): File {
        val zip = File(directory, "sync.zip")
        ZipOutputStream(zip.outputStream()).use { output ->
            output.putNextEntry(ZipEntry("manifest.json"))
            output.write(
                """
                {
                  "formatVersion": 1,
                  "exportedAtEpochMillis": 1790000000000,
                  "appVersion": "2.4.0",
                  "counts": {
                    "studyTasks": 1,
                    "contentCards": 0,
                    "attachments": 0
                  },
                  "tables": {
                    "study_tasks": [{
                      "id": "task-1",
                      "title": "数学复习",
                      "scheduledDate": "2026-10-26",
                      "startTime": "19:30",
                      "section": "NEW",
                      "isCompleted": false,
                      "spacedRepetitionEnabled": false,
                      "sourceType": "ORIGINAL"
                    }],
                    "task_content_cards": [],
                    "learning_snapshots": [],
                    "error_records": [],
                    "error_screenshots": [],
                    "week_summary_documents": []
                  }
                }
                """.trimIndent().toByteArray(Charsets.UTF_8),
            )
            output.closeEntry()
        }
        return zip
    }
}
