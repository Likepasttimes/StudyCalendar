package com.learningcalendar.desktop.sync

import java.io.File
import java.io.IOException

object DesktopDataLocation {
    const val ENV_DATA_DIRECTORY = "LEARNING_CALENDAR_DATA_DIR"
    const val CONFIG_FILE_NAME = "data-location.properties"

    fun resolve(): File {
        val localAppData = System.getenv("LOCALAPPDATA")
            ?.takeIf(String::isNotBlank)
            ?: System.getProperty("user.home")
        val legacyRoot = File(localAppData, "LearningCalendar")
        val configuredRoot = System.getenv(ENV_DATA_DIRECTORY)
            ?.takeIf(String::isNotBlank)
            ?.let(::File)
            ?: configFile(legacyRoot)
                .takeIf(File::isFile)
                ?.readText(Charsets.UTF_8)
                ?.let(::parseConfigDirectory)
            ?: legacyRoot

        val selectedRoot = configuredRoot.absoluteFile.normalize()
        require(selectedRoot.isAbsolute) { "数据目录必须是绝对路径" }
        if (!selectedRoot.exists() && !selectedRoot.mkdirs()) {
            throw IOException("无法创建数据目录：${selectedRoot.absolutePath}")
        }
        require(selectedRoot.isDirectory) {
            "配置的数据保存位置不是文件夹：${selectedRoot.absolutePath}"
        }
        migrateLegacyDataIfNeeded(legacyRoot, selectedRoot)
        return selectedRoot
    }

    internal fun parseConfigDirectory(configText: String): File? {
        val path = configText.lineSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith("data.root=", ignoreCase = true) }
            ?.substringAfter('=', "")
            ?.trim()
            .orEmpty()
        return path.takeIf(String::isNotBlank)
            ?.let(::decodeConfigValue)
            ?.let(::File)
    }

    internal fun migrateLegacyDataIfNeeded(
        legacyRoot: File,
        selectedRoot: File,
    ) {
        val legacy = legacyRoot.canonicalFile
        val selected = selectedRoot.canonicalFile
        if (legacy == selected) return

        val legacySnapshots = File(legacy, "snapshots")
        val selectedSnapshots = File(selected, "snapshots")
        val legacyHasSnapshots = legacySnapshots.listFiles()
            ?.any { it.isDirectory }
            ?: false
        val selectedHasSnapshots = selectedSnapshots.listFiles()
            ?.any { it.isDirectory }
            ?: false
        if (!legacyHasSnapshots || selectedHasSnapshots) return

        if (!selectedSnapshots.exists() && !selectedSnapshots.mkdirs()) {
            throw IOException("无法创建新数据目录")
        }
        legacySnapshots.copyRecursively(
            target = selectedSnapshots,
            overwrite = false,
        )
        val legacyPointer = File(legacy, "current-snapshot.txt")
        if (legacyPointer.isFile) {
            val currentDirectoryName = File(
                legacyPointer.readText(Charsets.UTF_8).trim(),
            ).name
            val migratedCurrent = File(selectedSnapshots, currentDirectoryName)
            if (migratedCurrent.isDirectory) {
                File(selected, "current-snapshot.txt")
                    .writeText(migratedCurrent.absolutePath, Charsets.UTF_8)
            }
        }
    }

    private fun configFile(legacyRoot: File): File =
        File(legacyRoot, CONFIG_FILE_NAME)

    private fun decodeConfigValue(value: String): String {
        val decoded = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            if (
                value[index] == '\\' &&
                index + 5 < value.length &&
                value[index + 1] == 'u'
            ) {
                val code = value.substring(index + 2, index + 6)
                    .toIntOrNull(16)
                if (code != null) {
                    decoded.append(code.toChar())
                    index += 6
                    continue
                }
            }
            decoded.append(value[index])
            index += 1
        }
        return decoded.toString()
    }
}
