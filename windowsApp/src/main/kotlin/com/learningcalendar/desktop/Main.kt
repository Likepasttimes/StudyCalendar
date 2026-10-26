package com.learningcalendar.desktop

import com.learningcalendar.desktop.sync.DesktopSyncServer
import com.learningcalendar.desktop.sync.DesktopSyncStore
import com.learningcalendar.desktop.ui.DesktopCalendarWindow
import java.awt.Font
import javax.swing.SwingUtilities
import javax.swing.UIManager

fun main() {
    System.setProperty("file.encoding", "UTF-8")
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    UIManager.put("Label.font", Font("Microsoft YaHei UI", Font.PLAIN, 14))
    UIManager.put("Button.font", Font("Microsoft YaHei UI", Font.PLAIN, 14))
    UIManager.put("TextArea.font", Font("Microsoft YaHei UI", Font.PLAIN, 14))

    SwingUtilities.invokeLater {
        runCatching {
            val store = DesktopSyncStore()
            val server = DesktopSyncServer(store).start()
            DesktopCalendarWindow(store, server).isVisible = true
        }.onFailure { error ->
            javax.swing.JOptionPane.showMessageDialog(
                null,
                error.message ?: "学习日历 Windows 版启动失败",
                "启动失败",
                javax.swing.JOptionPane.ERROR_MESSAGE,
            )
            System.exit(1)
        }
    }
}
