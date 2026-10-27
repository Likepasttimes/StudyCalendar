package com.learningcalendar.desktop.ui

import com.learningcalendar.desktop.model.SyncContentCard
import com.learningcalendar.desktop.model.SyncErrorAttachment
import com.learningcalendar.desktop.model.SyncErrorRecord
import com.learningcalendar.desktop.model.SyncLearningSnapshot
import com.learningcalendar.desktop.model.SyncSnapshot
import com.learningcalendar.desktop.model.SyncTask
import com.learningcalendar.desktop.model.SyncWeekDocument
import com.learningcalendar.desktop.sync.DesktopSyncServer
import com.learningcalendar.desktop.sync.DesktopSyncStore
import com.learningcalendar.desktop.sync.LoadedSync
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Desktop
import java.awt.Dialog
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileNameExtensionFilter

private val WarmBackground = Color(0xFFF7F2E8.toInt())
private val SurfaceWarm = Color(0xFFFFFDF8.toInt())
private val DeepBlue = Color(0xFF183247.toInt())
private val MutedBlue = Color(0xFF647985.toInt())
private val Sage = Color(0xFF7E9C88.toInt())
private val SageSoft = Color(0xFFE8F0E8.toInt())
private val SageDark = Color(0xFF486A57.toInt())
private val Amber = Color(0xFFB8782B.toInt())
private val CardOutline = Color(0xFFE2DDD2.toInt())
private val Sidebar = Color(0xFF1D3A4C.toInt())

private val titleFont = Font("Microsoft YaHei UI", Font.BOLD, 24)
private val headingFont = Font("Microsoft YaHei UI", Font.BOLD, 18)
private val bodyFont = Font("Microsoft YaHei UI", Font.PLAIN, 14)
private val smallFont = Font("Microsoft YaHei UI", Font.PLAIN, 12)

enum class DesktopSection(
    val label: String,
    val subtitle: String,
) {
    TASKS("学习任务", "按日期查看任务、内容卡片与验收状态"),
    WEEKLY("周总结", "按周查看上传的 docx 与 pdf 报告"),
    SYNC("电脑同步", "连接手机并把全部本地数据保存到这台电脑"),
}

class DesktopCalendarWindow(
    private val store: DesktopSyncStore,
    private val server: DesktopSyncServer,
) : JFrame("学习日历") {
    private val navigationButtons = mutableMapOf<DesktopSection, JButton>()
    private val headerTitle = JLabel()
    private val headerSubtitle = JLabel()
    private val contentHost = JPanel(BorderLayout())
    private var selectedSection = DesktopSection.TASKS

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val targetWidth = minOf(1000, (screenSize.width * 0.90).toInt())
        val targetHeight = minOf(600, (screenSize.height * 0.82).toInt())
        minimumSize = Dimension(
            minOf(860, targetWidth),
            minOf(520, targetHeight),
        )
        size = Dimension(targetWidth, targetHeight)
        setLocation(20, 20)
        contentPane = buildRoot()

        server.onSyncReceived = { _ ->
            SwingUtilities.invokeLater {
                select(selectedSection)
                refreshNavigationFooter()
            }
        }
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(event: WindowEvent) {
                server.close()
                dispose()
                System.exit(0)
            }
        })

        select(DesktopSection.TASKS)
    }

    private fun buildRoot(): JPanel {
        val root = RatioSplitPanel()
        root.add(buildSidebar())
        root.add(buildContentArea())
        return root
    }

    private fun buildSidebar(): JPanel {
        val sidebar = JPanel()
        sidebar.layout = BoxLayout(sidebar, BoxLayout.Y_AXIS)
        sidebar.background = Sidebar
        sidebar.border = EmptyBorder(28, 20, 22, 20)
        sidebar.preferredSize = Dimension(210, 0)

        val title = JLabel("学习日历")
        title.font = Font("Microsoft YaHei UI", Font.BOLD, 23)
        title.foreground = Color.WHITE
        title.alignmentX = Component.LEFT_ALIGNMENT
        sidebar.add(title)
        sidebar.add(Box.createVerticalStrut(28))

        DesktopSection.entries.forEach { section ->
            val button = navButton(section)
            navigationButtons[section] = button
            sidebar.add(button)
            sidebar.add(Box.createVerticalStrut(8))
        }

        sidebar.add(Box.createVerticalGlue())
        val footer = JLabel(
            "<html><span style='color:#B9C8CF'>本地保存</span><br>" +
                "<span style='color:#FFFFFF'>不经过云端</span></html>",
        )
        footer.font = smallFont
        footer.alignmentX = Component.LEFT_ALIGNMENT
        footer.border = EmptyBorder(12, 10, 0, 0)
        sidebar.add(footer)
        return sidebar
    }

    private fun navButton(section: DesktopSection): JButton =
        object : JButton("  ${section.label}") {
            override fun paintComponent(graphics: Graphics) {
                val g = graphics.create() as Graphics2D
                g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON,
                )
                g.color = if (background == Sage) Sage else Sidebar
                g.fillRoundRect(0, 0, width, height, 16, 16)
                g.dispose()
                super.paintComponent(graphics)
            }
        }.apply {
            isContentAreaFilled = false
            isOpaque = false
            isBorderPainted = false
            isFocusPainted = false
            isFocusable = false
            horizontalAlignment = SwingConstants.LEFT
            foreground = Color.WHITE
            font = Font("Microsoft YaHei UI", Font.PLAIN, 15)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            maximumSize = Dimension(Int.MAX_VALUE, 48)
            preferredSize = Dimension(170, 48)
            alignmentX = Component.LEFT_ALIGNMENT
            addActionListener { select(section) }
        }

    private fun buildContentArea(): JPanel {
        val wrapper = JPanel(BorderLayout())
        wrapper.background = WarmBackground
        wrapper.border = EmptyBorder(26, 30, 26, 30)

        val header = JPanel(BorderLayout())
        header.isOpaque = false
        header.border = EmptyBorder(0, 2, 18, 2)
        val titles = JPanel(FlowLayout(FlowLayout.LEFT, 12, 0))
        titles.isOpaque = false
        headerTitle.font = titleFont
        headerTitle.foreground = DeepBlue
        headerSubtitle.font = bodyFont
        headerSubtitle.foreground = MutedBlue
        headerSubtitle.border = EmptyBorder(7, 0, 0, 0)
        titles.add(headerTitle)
        titles.add(headerSubtitle)
        header.add(titles, BorderLayout.WEST)
        wrapper.add(header, BorderLayout.NORTH)

        contentHost.background = WarmBackground
        wrapper.add(contentHost, BorderLayout.CENTER)
        return wrapper
    }

    private fun select(section: DesktopSection) {
        selectedSection = section
        navigationButtons.forEach { (item, button) ->
            button.background = if (item == section) Sage else Sidebar
            button.foreground = Color.WHITE
        }
        headerTitle.text = section.label
        headerSubtitle.text = section.subtitle
        contentHost.removeAll()
        val content = when (section) {
            DesktopSection.TASKS -> TasksPage(store.current, ::openAttachment)
            DesktopSection.WEEKLY -> WeeklyPage(
                store = store,
                openAttachment = ::openAttachment,
                onDataChanged = { select(DesktopSection.WEEKLY) },
            )
            DesktopSection.SYNC -> SyncPage(store, server, ::select)
        }
        contentHost.add(content, BorderLayout.CENTER)
        contentHost.revalidate()
        contentHost.repaint()
        refreshNavigationFooter()
    }

    private fun refreshNavigationFooter() {
        val status = store.current
        navigationButtons[DesktopSection.SYNC]?.toolTipText = if (status == null) {
            "等待手机首次同步"
        } else {
            "上次同步：${formatDateTime(status.snapshot.exportedAtEpochMillis)}"
        }
    }

    private fun openAttachment(relativePath: String, displayName: String) {
        val file = store.resolveAttachment(relativePath)
        if (file == null) {
            JOptionPane.showMessageDialog(
                this,
                "同步数据中没有找到文件：$displayName",
                "文件不可用",
                JOptionPane.WARNING_MESSAGE,
            )
            return
        }
        runCatching {
            if (!Desktop.isDesktopSupported()) error("当前系统不支持打开文件")
            Desktop.getDesktop().open(file)
        }.onFailure { error ->
            JOptionPane.showMessageDialog(
                this,
                error.message ?: "无法打开文件",
                "打开失败",
                JOptionPane.ERROR_MESSAGE,
            )
        }
    }
}

private class RatioSplitPanel : JPanel(null) {
    init {
        background = WarmBackground
    }

    override fun doLayout() {
        if (componentCount < 2) return
        val sidebarWidth = (width / 6.0).toInt().coerceAtLeast(170)
        getComponent(0).setBounds(0, 0, sidebarWidth, height)
        getComponent(1).setBounds(sidebarWidth, 0, width - sidebarWidth, height)
    }
}

private class TasksPage(
    private val loaded: LoadedSync?,
    private val openAttachment: (String, String) -> Unit,
) : JPanel(BorderLayout()) {
    private var month = YearMonth.now()
    private var selectedDate = LocalDate.now()
    private val monthLabel = JLabel()
    private val calendar = CalendarGridPanel()
    private val calendarBody = JPanel(BorderLayout())
    private val weekStrip = WeekStripPanel()
    private var calendarCollapsed = false
    private var toggleCalendarButton: JButton? = null
    private val detailHost = JPanel(BorderLayout())
    private val detailScroll = scrollable(detailHost)

    init {
        background = WarmBackground
        val snapshot = loaded?.snapshot
        if (snapshot == null) {
            add(emptyState("还没有电脑同步数据", "打开手机端“电脑同步”并完成一次连接。"))
        } else {
            val container = JPanel(BorderLayout(0, 16))
            container.isOpaque = false
            container.add(buildCalendarCard(snapshot), BorderLayout.NORTH)

            detailHost.isOpaque = false
            container.add(detailScroll, BorderLayout.CENTER)
            add(container, BorderLayout.CENTER)

            calendar.snapshot = snapshot
            calendar.month = month
            calendar.selectedDate = selectedDate
            calendar.onDateSelected = { date ->
                selectedDate = date
                month = YearMonth.from(date)
                refreshCalendar()
            }
            weekStrip.onDateSelected = { date ->
                selectedDate = date
                month = YearMonth.from(date)
                refreshCalendar()
            }
            refreshCalendar()
        }
    }

    private fun buildCalendarCard(snapshot: SyncSnapshot): JPanel {
        val card = RoundedPanel(SurfaceWarm, CardOutline, 22)
        card.layout = BorderLayout(0, 10)
        card.border = EmptyBorder(16, 18, 16, 18)

        val toolbar = JPanel(BorderLayout())
        toolbar.isOpaque = false
        val controls = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        controls.isOpaque = false
        controls.add(actionButton("上月") {
            month = month.minusMonths(1)
            refreshCalendar()
        })
        controls.add(actionButton("下月") {
            month = month.plusMonths(1)
            refreshCalendar()
        })
        controls.add(actionButton("回到今天") {
            month = YearMonth.now()
            selectedDate = LocalDate.now()
            refreshCalendar()
        })
        toggleCalendarButton = actionButton("收起日历") {
            calendarCollapsed = !calendarCollapsed
            refreshCalendar()
        }
        controls.add(toggleCalendarButton)
        monthLabel.font = headingFont
        monthLabel.foreground = DeepBlue
        toolbar.add(controls, BorderLayout.WEST)
        toolbar.add(monthLabel, BorderLayout.EAST)
        card.add(toolbar, BorderLayout.NORTH)
        calendar.preferredSize = Dimension(620, 290)
        calendarBody.isOpaque = false
        card.add(calendarBody, BorderLayout.CENTER)

        if (snapshot.tasks.isEmpty()) {
            val empty = JLabel("这个同步快照中没有学习任务。")
            empty.font = smallFont
            empty.foreground = MutedBlue
            card.add(empty, BorderLayout.SOUTH)
        }
        return card
    }

    private fun refreshCalendar() {
        val snapshot = loaded?.snapshot ?: return
        calendar.month = month
        calendar.selectedDate = selectedDate
        monthLabel.text = "${month.year} 年 ${month.monthValue} 月"
        calendar.repaint()
        calendarBody.removeAll()
        if (calendarCollapsed) {
            weekStrip.snapshot = snapshot
            weekStrip.selectedDate = selectedDate
            weekStrip.preferredSize = Dimension(620, 86)
            calendarBody.add(weekStrip, BorderLayout.CENTER)
        } else {
            calendarBody.add(calendar, BorderLayout.CENTER)
        }
        calendarBody.revalidate()
        calendarBody.repaint()
        toggleCalendarButton?.text = if (calendarCollapsed) "展开日历" else "收起日历"

        detailHost.removeAll()
        val tasks = snapshot.tasks
            .filter { it.scheduledDate == selectedDate }
            .sortedWith(
                compareBy<SyncTask> { it.startTime == null }
                    .thenBy { it.startTime.orEmpty() }
                    .thenBy { it.title },
            )
        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.background = WarmBackground
        content.border = EmptyBorder(0, 2, 16, 2)

        val dateTitle = JLabel(
            "${selectedDate.year} 年 ${selectedDate.monthValue} 月 " +
                "${selectedDate.dayOfMonth} 日 · ${weekdayText(selectedDate.dayOfWeek)}",
        )
        dateTitle.font = headingFont
        dateTitle.foreground = DeepBlue
        dateTitle.alignmentX = Component.LEFT_ALIGNMENT
        content.add(dateTitle)
        content.add(Box.createVerticalStrut(12))

        if (tasks.isEmpty()) {
            content.add(emptyInline("当天没有任务卡"))
        } else {
            tasks.forEach { task ->
                content.add(taskCard(snapshot, task))
                content.add(Box.createVerticalStrut(12))
            }
        }
        detailHost.add(content, BorderLayout.NORTH)
        detailHost.revalidate()
        detailHost.repaint()
        SwingUtilities.invokeLater {
            detailScroll.verticalScrollBar.value = 0
        }
    }

    private fun taskCard(snapshot: SyncSnapshot, task: SyncTask): JPanel {
        val card = RoundedPanel(SurfaceWarm, CardOutline, 18)
        card.layout = BoxLayout(card, BoxLayout.Y_AXIS)
        card.border = EmptyBorder(15, 16, 15, 16)
        card.alignmentX = Component.LEFT_ALIGNMENT
        card.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)

        val headerRow = JPanel(BorderLayout(12, 0))
        headerRow.isOpaque = false
        headerRow.alignmentX = Component.LEFT_ALIGNMENT
        headerRow.add(
            label(task.title, Font("Microsoft YaHei UI", Font.BOLD, 16), DeepBlue),
            BorderLayout.CENTER,
        )
        headerRow.add(
            actionButton("任务详情") {
                showTaskDetailDialog(snapshot, task)
            },
            BorderLayout.EAST,
        )
        card.add(headerRow)
        card.add(Box.createVerticalStrut(6))

        val badges = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0))
        badges.isOpaque = false
        badges.alignmentX = Component.LEFT_ALIGNMENT
        badges.add(pill(task.startTime ?: "未填写时间"))
        badges.add(
            pill(
                when (task.section) {
                    "REVIEW" -> "复习专区"
                    else -> "新任务专区"
                },
            ),
        )
        if (task.isCompleted) badges.add(statusPill("已完成", SageDark, SageSoft))
        if (task.spacedRepetitionEnabled) badges.add(statusPill("已加入二三七复习", Amber))
        card.add(badges)
        return card
    }

    private fun showTaskDetailDialog(snapshot: SyncSnapshot, task: SyncTask) {
        val owner = SwingUtilities.getWindowAncestor(this)
        val dialog = JDialog(owner, "任务详情", Dialog.ModalityType.APPLICATION_MODAL)
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        dialog.background = WarmBackground

        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.background = WarmBackground
        content.border = EmptyBorder(22, 24, 24, 24)

        val detailHeader = JPanel(BorderLayout(12, 0))
        detailHeader.isOpaque = false
        detailHeader.alignmentX = Component.LEFT_ALIGNMENT
        detailHeader.add(
            label(task.title, Font("Microsoft YaHei UI", Font.BOLD, 22), DeepBlue),
            BorderLayout.CENTER,
        )
        if (reviewChain(snapshot, task).isNotEmpty()) {
            detailHeader.add(
                actionButton("历史记录") {
                    showReviewHistoryDialog(dialog, snapshot, task)
                },
                BorderLayout.EAST,
            )
        }
        content.add(detailHeader)
        content.add(Box.createVerticalStrut(8))
        content.add(
            label(
                "${task.scheduledDate.year} 年 ${task.scheduledDate.monthValue} 月 " +
                    "${task.scheduledDate.dayOfMonth} 日 · " +
                    weekdayText(task.scheduledDate.dayOfWeek) +
                    " · " + (task.startTime ?: "未填写时间"),
                smallFont,
                MutedBlue,
            ).alignLeft(),
        )
        content.add(Box.createVerticalStrut(14))

        val cards = snapshot.contentCards
            .filter { it.taskId == task.id }
            .sortedBy { it.sortOrder }
        if (cards.isEmpty()) {
            content.add(emptyInline("这个任务还没有内容卡片"))
        } else {
            cards.forEachIndexed { index, contentCard ->
                content.add(contentCardBlock(index + 1, contentCard, snapshot))
                if (index < cards.lastIndex) {
                    content.add(Box.createVerticalStrut(10))
                }
            }
        }

        dialog.add(scrollable(content), BorderLayout.CENTER)
        val closeRow = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
        closeRow.background = WarmBackground
        closeRow.border = EmptyBorder(8, 20, 14, 20)
        closeRow.add(actionButton("关闭") { dialog.dispose() })
        dialog.add(closeRow, BorderLayout.SOUTH)
        dialog.setSize(760, 620)
        dialog.setLocationRelativeTo(owner)
        dialog.isVisible = true
    }

    private fun reviewChain(snapshot: SyncSnapshot, task: SyncTask): List<SyncTask> {
        val sourceId = when (task.sourceType) {
            "AUTO_REVIEW" -> task.sourceTaskId
            "ORIGINAL" -> task.id
            else -> null
        } ?: return emptyList()
        val source = snapshot.tasks.firstOrNull {
            it.id == sourceId && it.sourceType == "ORIGINAL"
        } ?: return emptyList()
        val reviews = snapshot.tasks
            .filter { it.sourceTaskId == sourceId && it.sourceType == "AUTO_REVIEW" }
            .sortedBy { it.reviewSequence ?: Int.MAX_VALUE }
        return buildList {
            add(source)
            addAll(reviews)
        }
    }

    private fun showReviewHistoryDialog(
        owner: Dialog?,
        snapshot: SyncSnapshot,
        task: SyncTask,
    ) {
        val chain = reviewChain(snapshot, task)
        val source = chain.firstOrNull { it.sourceType == "ORIGINAL" } ?: return
        val reviews = chain.filter { it.sourceType == "AUTO_REVIEW" }
        val dialog = JDialog(owner, "复习历史", Dialog.ModalityType.APPLICATION_MODAL)
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        dialog.background = WarmBackground

        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.background = WarmBackground
        content.border = EmptyBorder(18, 20, 18, 20)
        content.add(label("复习历史", Font("Microsoft YaHei UI", Font.BOLD, 21), DeepBlue))
        content.add(Box.createVerticalStrut(5))
        content.add(label("每天会保留此前已经学习过的任务记录。", smallFont, MutedBlue))
        content.add(Box.createVerticalStrut(12))

        val today = LocalDate.now()
        val reachedReviews = reviews.filter { !it.scheduledDate.isAfter(today) }
        val availableDays = buildList {
            add(1)
            listOf(2, 3, 7).forEach { day ->
                if (reviews.any {
                        it.reviewSequence == day && !it.scheduledDate.isAfter(today)
                    }
                ) {
                    add(day)
                }
            }
        }
        availableDays.forEach { day ->
            content.add(label("第 $day 天", headingFont, SageDark).alignLeft())
            content.add(Box.createVerticalStrut(6))
            val visible = buildList {
                add(source)
                if (day > 1) {
                    addAll(
                        reachedReviews.filter {
                            (it.reviewSequence ?: Int.MAX_VALUE) <= day
                        },
                    )
                }
            }
            visible.forEach { historyTask ->
                val row = RoundedPanel(SurfaceWarm, CardOutline, 14)
                row.layout = BoxLayout(row, BoxLayout.Y_AXIS)
                row.border = EmptyBorder(9, 12, 9, 12)
                row.alignmentX = Component.LEFT_ALIGNMENT
                row.add(
                    label(
                        historyTask.title,
                        Font("Microsoft YaHei UI", Font.BOLD, 14),
                        DeepBlue,
                    ).alignLeft(),
                )
                row.add(
                    label(
                        "${historyTask.scheduledDate.monthValue} 月 " +
                            "${historyTask.scheduledDate.dayOfMonth} 日 · " +
                            if (historyTask.sourceType == "ORIGINAL") {
                                "原始任务"
                            } else {
                                "第 ${historyTask.reviewSequence} 天复习任务"
                            },
                        smallFont,
                        MutedBlue,
                    ).alignLeft(),
                )
                installClick(row) {
                    dialog.dispose()
                    SwingUtilities.invokeLater {
                        showTaskDetailDialog(snapshot, historyTask)
                    }
                }
                row.maximumSize = Dimension(Int.MAX_VALUE, row.preferredSize.height)
                row.minimumSize = Dimension(0, row.preferredSize.height)
                content.add(row)
                content.add(Box.createVerticalStrut(7))
            }
            content.add(Box.createVerticalStrut(8))
        }
        dialog.add(scrollable(content), BorderLayout.CENTER)
        val close = JPanel(FlowLayout(FlowLayout.RIGHT))
        close.background = WarmBackground
        close.add(actionButton("关闭") { dialog.dispose() })
        dialog.add(close, BorderLayout.SOUTH)
        dialog.setSize(720, 620)
        dialog.setLocationRelativeTo(owner)
        dialog.isVisible = true
    }

    private fun contentCardBlock(
        sequence: Int,
        card: SyncContentCard,
        snapshot: SyncSnapshot,
    ): JPanel {
        val block = JPanel()
        block.layout = BoxLayout(block, BoxLayout.Y_AXIS)
        block.isOpaque = false
        block.alignmentX = Component.LEFT_ALIGNMENT
        block.add(
            label("内容卡片 $sequence", Font("Microsoft YaHei UI", Font.BOLD, 14), SageDark)
                .alignLeft(),
        )
        block.add(Box.createVerticalStrut(5))
        block.add(multilineLabel("任务目标：${card.objective}"))
        block.add(multilineLabel("初始动作：${card.initialAction}"))
        block.add(multilineLabel("验收标准：${card.acceptanceCriteria}"))
        val status = buildString {
            append(if (card.acceptanceCompleted) "验收已完成" else "验收未完成")
            card.masteryLevel?.let { append(" · 掌握度：${masteryText(it)}") }
        }
        block.add(
            label(status, smallFont, if (card.acceptanceCompleted) SageDark else MutedBlue)
                .alignLeft(),
        )

        val snapshots = snapshot.learningSnapshots
            .filter { it.cardId == card.id }
            .sortedBy { it.createdAtEpochMillis }
        if (snapshots.isNotEmpty()) {
            block.add(Box.createVerticalStrut(10))
            block.add(label("学习快照", Font("Microsoft YaHei UI", Font.BOLD, 13), SageDark))
            snapshots.forEach { item ->
                block.add(
                    actionButton("${kindText(item.kind)} · ${item.displayName}") {
                        openAttachment(item.relativePath, item.displayName)
                    }.alignLeft(),
                )
            }
        }

        val errors = snapshot.errorRecords.filter { it.cardId == card.id }
        if (errors.isNotEmpty()) {
            block.add(Box.createVerticalStrut(12))
            block.add(label("错误记录", Font("Microsoft YaHei UI", Font.BOLD, 13), Amber))
            errors.forEach { error ->
                block.add(
                    label(error.errorPoint, Font("Microsoft YaHei UI", Font.BOLD, 13), DeepBlue)
                        .alignLeft(),
                )
                if (error.description.isNotBlank()) {
                    block.add(multilineLabel(error.description))
                }
                snapshot.errorAttachments
                    .filter { it.errorRecordId == error.id }
                    .forEach { attachment ->
                        block.add(
                            actionButton("打开附件 · ${attachment.displayName}") {
                                openAttachment(attachment.relativePath, attachment.displayName)
                            }.alignLeft(),
                        )
                    }
            }
        }
        return block
    }
}

private class SnapshotsPage(
    private val loaded: LoadedSync?,
    private val openAttachment: (String, String) -> Unit,
) : JPanel(BorderLayout()) {
    init {
        background = WarmBackground
        val snapshot = loaded?.snapshot
        if (snapshot == null || snapshot.learningSnapshots.isEmpty()) {
            add(emptyState("还没有学习快照", "完成电脑同步后，学习照片、录音和文件会显示在这里。"))
        } else {
            val list = verticalList()
            snapshot.learningSnapshots
                .sortedByDescending { it.createdAtEpochMillis }
                .forEach { item ->
                    list.add(attachmentCard(snapshot, item, openAttachment))
                    list.add(Box.createVerticalStrut(10))
                }
            add(scrollable(list), BorderLayout.CENTER)
        }
    }

    private fun attachmentCard(
        snapshot: SyncSnapshot,
        item: SyncLearningSnapshot,
        open: (String, String) -> Unit,
    ): JPanel {
        val card = RoundedPanel(SurfaceWarm, CardOutline, 18)
        card.layout = BorderLayout(14, 0)
        card.border = EmptyBorder(15, 16, 15, 16)
        val text = JPanel()
        text.layout = BoxLayout(text, BoxLayout.Y_AXIS)
        text.isOpaque = false
        text.add(label(item.displayName, Font("Microsoft YaHei UI", Font.BOLD, 15), DeepBlue))
        val cardModel = snapshot.contentCards.firstOrNull { it.id == item.cardId }
        val task = snapshot.tasks.firstOrNull { it.id == cardModel?.taskId }
        text.add(label("${kindText(item.kind)} · ${task?.title ?: "内容卡片"}", smallFont, MutedBlue))
        text.add(label(formatDateTime(item.createdAtEpochMillis), smallFont, MutedBlue))
        card.add(text, BorderLayout.CENTER)
        card.add(primaryButton("打开") { open(item.relativePath, item.displayName) }, BorderLayout.EAST)
        return card
    }
}

private class WeeklyPage(
    private val store: DesktopSyncStore,
    private val openAttachment: (String, String) -> Unit,
    private val onDataChanged: () -> Unit,
) : JPanel(BorderLayout()) {
    private var weekStart = LocalDate.now().with(
        java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
    )
    private val weekLabel = JLabel()
    private val contentHost = JPanel(BorderLayout())

    init {
        background = WarmBackground
        val toolbar = JPanel(BorderLayout())
        toolbar.isOpaque = false
        toolbar.border = EmptyBorder(0, 2, 14, 2)
        val controls = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        controls.isOpaque = false
        controls.add(actionButton("上一周") {
            weekStart = weekStart.minusWeeks(1)
            refresh()
        })
        controls.add(actionButton("下一周") {
            weekStart = weekStart.plusWeeks(1)
            refresh()
        })
        controls.add(actionButton("回到本周") {
            weekStart = LocalDate.now().with(
                java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
            )
            refresh()
        })
        controls.add(
            primaryButton("上传周总结") {
                chooseAndUploadSummary()
            },
        )
        weekLabel.font = headingFont
        weekLabel.foreground = DeepBlue
        toolbar.add(controls, BorderLayout.WEST)
        toolbar.add(weekLabel, BorderLayout.EAST)
        add(toolbar, BorderLayout.NORTH)

        contentHost.background = WarmBackground
        add(contentHost, BorderLayout.CENTER)
        refresh()
    }

    private fun refresh() {
        val snapshot = store.current?.snapshot
        weekLabel.text = "${weekStart.monthValue} 月 ${weekStart.dayOfMonth} 日 · " +
            "${weekStart.plusDays(6).monthValue} 月 ${weekStart.plusDays(6).dayOfMonth} 日"
        contentHost.removeAll()
        if (snapshot == null) {
            contentHost.add(
                emptyState("还没有电脑同步数据", "请先在手机端完成一次电脑同步。"),
                BorderLayout.CENTER,
            )
        } else {
            val endDate = weekStart.plusDays(6)
            val weekTasks = snapshot.tasks
                .filter { it.scheduledDate in weekStart..endDate }
                .sortedWith(
                    compareBy<SyncTask> { it.scheduledDate }
                        .thenBy { it.startTime == null }
                        .thenBy { it.startTime.orEmpty() }
                        .thenBy { it.title },
                )
            val documents = snapshot.weekDocuments
                .filter { it.weekStartDate == weekStart }
                .sortedBy { it.uploadedAtEpochMillis }

            val page = verticalList()
            page.add(sectionTitle("本周任务", "${weekTasks.size} 个任务卡"))
            page.add(Box.createVerticalStrut(8))
            if (weekTasks.isEmpty()) {
                page.add(emptyInline("这一周没有任务卡"))
            } else {
                weekTasks.forEach { task ->
                    page.add(weekTaskCard(snapshot, task))
                    page.add(Box.createVerticalStrut(8))
                }
            }
            page.add(Box.createVerticalStrut(12))
            page.add(sectionTitle("周总结报告", "${documents.size} 份文档"))
            page.add(Box.createVerticalStrut(8))
            if (documents.isEmpty()) {
                page.add(emptyInline("这一周还没有上传周总结"))
            } else {
                documents.forEach { document ->
                    page.add(weekCard(document))
                    page.add(Box.createVerticalStrut(8))
                }
            }
            contentHost.add(scrollable(page), BorderLayout.CENTER)
        }
        contentHost.revalidate()
        contentHost.repaint()
    }

    private fun sectionTitle(title: String, summary: String): JPanel =
        JPanel(BorderLayout(10, 0)).apply {
            isOpaque = false
            alignmentX = Component.LEFT_ALIGNMENT
            add(label(title, headingFont, DeepBlue), BorderLayout.WEST)
            add(label(summary, smallFont, MutedBlue), BorderLayout.CENTER)
        }

    private fun weekTaskCard(snapshot: SyncSnapshot, task: SyncTask): JPanel {
        val card = RoundedPanel(SurfaceWarm, CardOutline, 18)
        card.layout = BorderLayout(12, 0)
        card.border = EmptyBorder(13, 15, 13, 15)
        card.alignmentX = Component.LEFT_ALIGNMENT
        val texts = JPanel()
        texts.layout = BoxLayout(texts, BoxLayout.Y_AXIS)
        texts.isOpaque = false
        texts.add(label(task.title, Font("Microsoft YaHei UI", Font.BOLD, 15), DeepBlue))
        texts.add(
            label(
                "${task.scheduledDate.monthValue} 月 ${task.scheduledDate.dayOfMonth} 日 · " +
                    "${weekdayText(task.scheduledDate.dayOfWeek)} · " +
                    (task.startTime ?: "未填写时间"),
                smallFont,
                MutedBlue,
            ),
        )
        card.add(texts, BorderLayout.CENTER)
        card.add(
            actionButton("查看") {
                showWeeklyTaskDetail(snapshot, task)
            },
            BorderLayout.EAST,
        )
        return card
    }

    private fun weekCard(document: SyncWeekDocument): JPanel {
        val card = RoundedPanel(SurfaceWarm, CardOutline, 18)
        card.layout = BorderLayout(12, 0)
        card.border = EmptyBorder(13, 15, 13, 15)
        card.alignmentX = Component.LEFT_ALIGNMENT
        val texts = JPanel()
        texts.layout = BoxLayout(texts, BoxLayout.Y_AXIS)
        texts.isOpaque = false
        texts.add(
            label(
                document.displayName,
                Font("Microsoft YaHei UI", Font.BOLD, 15),
                DeepBlue,
            ),
        )
        texts.add(label(formatDateTime(document.uploadedAtEpochMillis), smallFont, MutedBlue))
        card.add(texts, BorderLayout.CENTER)
        val actions = JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0))
        actions.isOpaque = false
        actions.add(primaryButton("打开") {
            openAttachment(document.relativePath, document.displayName)
        })
        actions.add(actionButton("删除") {
            confirmDeleteDocument(document)
        })
        card.add(actions, BorderLayout.EAST)
        return card
    }

    private fun chooseAndUploadSummary() {
        if (store.current == null) {
            JOptionPane.showMessageDialog(
                this,
                "请先完成一次手机同步。",
                "暂无数据",
                JOptionPane.INFORMATION_MESSAGE,
            )
            return
        }
        val chooser = JFileChooser().apply {
            dialogTitle = "选择周总结文档"
            fileFilter = FileNameExtensionFilter("Word 或 PDF（*.docx, *.pdf）", "docx", "pdf")
            isAcceptAllFileFilterUsed = false
        }
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        runCatching {
            store.importWeekSummaryDocument(weekStart, chooser.selectedFile)
        }.onSuccess {
            onDataChanged()
        }.onFailure { error ->
            JOptionPane.showMessageDialog(
                this,
                error.message ?: "周总结上传失败",
                "上传失败",
                JOptionPane.ERROR_MESSAGE,
            )
        }
    }

    private fun confirmDeleteDocument(document: SyncWeekDocument) {
        val confirmed = JOptionPane.showConfirmDialog(
            this,
            "确定删除“${document.displayName}”吗？删除后无法通过普通返回恢复。",
            "删除周总结",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
        ) == JOptionPane.YES_OPTION
        if (!confirmed) return
        runCatching {
            store.deleteWeekSummaryDocument(document.id)
        }.onSuccess {
            onDataChanged()
        }.onFailure { error ->
            JOptionPane.showMessageDialog(
                this,
                error.message ?: "周总结删除失败",
                "删除失败",
                JOptionPane.ERROR_MESSAGE,
            )
        }
    }

    private fun showWeeklyTaskDetail(snapshot: SyncSnapshot, task: SyncTask) {
        val owner = SwingUtilities.getWindowAncestor(this)
        val dialog = JDialog(owner, "任务详情", Dialog.ModalityType.APPLICATION_MODAL)
        dialog.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        dialog.background = WarmBackground
        val content = JPanel()
        content.layout = BoxLayout(content, BoxLayout.Y_AXIS)
        content.background = WarmBackground
        content.border = EmptyBorder(20, 22, 20, 22)
        content.add(label(task.title, Font("Microsoft YaHei UI", Font.BOLD, 21), DeepBlue))
        content.add(Box.createVerticalStrut(10))
        val cards = snapshot.contentCards
            .filter { it.taskId == task.id }
            .sortedBy { it.sortOrder }
        if (cards.isEmpty()) {
            content.add(emptyInline("这个任务还没有内容卡片"))
        } else {
            cards.forEachIndexed { index, card ->
                val panel = RoundedPanel(SurfaceWarm, CardOutline, 18)
                panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
                panel.border = EmptyBorder(14, 16, 14, 16)
                panel.alignmentX = Component.LEFT_ALIGNMENT
                panel.add(
                    label("内容卡片 ${index + 1}", Font("Microsoft YaHei UI", Font.BOLD, 15), SageDark)
                        .alignLeft(),
                )
                panel.add(multilineLabel("任务目标：${card.objective}"))
                panel.add(multilineLabel("初始动作：${card.initialAction}"))
                panel.add(multilineLabel("验收标准：${card.acceptanceCriteria}"))
                panel.add(
                    label(
                        buildString {
                            append(if (card.acceptanceCompleted) "验收已完成" else "验收未完成")
                            card.masteryLevel?.let { append(" · 掌握度：${masteryText(it)}") }
                        },
                        smallFont,
                        MutedBlue,
                    ).alignLeft(),
                )
                snapshot.learningSnapshots.filter { it.cardId == card.id }.forEach { item ->
                    panel.add(
                        actionButton("学习快照 · ${item.displayName}") {
                            openAttachment(item.relativePath, item.displayName)
                        }.alignLeft(),
                    )
                }
                snapshot.errorRecords.filter { it.cardId == card.id }.forEach { error ->
                    panel.add(label("错误记录：${error.errorPoint}", bodyFont, Amber).alignLeft())
                    snapshot.errorAttachments
                        .filter { it.errorRecordId == error.id }
                        .forEach { attachment ->
                            panel.add(
                                actionButton("错误附件 · ${attachment.displayName}") {
                                    openAttachment(attachment.relativePath, attachment.displayName)
                                }.alignLeft(),
                            )
                        }
                }
                content.add(panel)
                content.add(Box.createVerticalStrut(10))
            }
        }
        dialog.add(scrollable(content), BorderLayout.CENTER)
        val close = JPanel(FlowLayout(FlowLayout.RIGHT))
        close.background = WarmBackground
        close.add(actionButton("关闭") { dialog.dispose() })
        dialog.add(close, BorderLayout.SOUTH)
        dialog.setSize(720, 600)
        dialog.setLocationRelativeTo(owner)
        dialog.isVisible = true
    }
}

private class ErrorsPage(
    private val loaded: LoadedSync?,
    private val openAttachment: (String, String) -> Unit,
) : JPanel(BorderLayout()) {
    init {
        background = WarmBackground
        val snapshot = loaded?.snapshot
        if (snapshot == null || snapshot.errorRecords.isEmpty()) {
            add(emptyState("还没有错误记录", "手机端新增错误记录并同步后，可在这里查看。"))
        } else {
            val list = verticalList()
            snapshot.errorRecords
                .sortedByDescending { it.createdAtEpochMillis }
                .forEach { record ->
                    list.add(errorCard(snapshot, record, openAttachment))
                    list.add(Box.createVerticalStrut(10))
                }
            add(scrollable(list), BorderLayout.CENTER)
        }
    }

    private fun errorCard(
        snapshot: SyncSnapshot,
        record: SyncErrorRecord,
        open: (String, String) -> Unit,
    ): JPanel {
        val card = RoundedPanel(SurfaceWarm, CardOutline, 18)
        card.layout = BoxLayout(card, BoxLayout.Y_AXIS)
        card.border = EmptyBorder(15, 16, 15, 16)
        card.add(label(record.errorPoint, Font("Microsoft YaHei UI", Font.BOLD, 16), DeepBlue))
        if (record.description.isNotBlank()) {
            card.add(multilineLabel(record.description))
        }
        val contextCard = snapshot.contentCards.firstOrNull { it.id == record.cardId }
        val task = snapshot.tasks.firstOrNull { it.id == contextCard?.taskId }
        card.add(label("关联：${task?.title ?: "任务内容卡片"}", smallFont, MutedBlue))
        card.add(label(formatDateTime(record.createdAtEpochMillis), smallFont, MutedBlue))

        val attachments = snapshot.errorAttachments
            .filter { it.errorRecordId == record.id }
        if (attachments.isNotEmpty()) {
            card.add(Box.createVerticalStrut(9))
            attachments.forEach { attachment ->
                card.add(errorAttachmentRow(attachment, open))
            }
        }
        return card
    }

    private fun errorAttachmentRow(
        attachment: SyncErrorAttachment,
        open: (String, String) -> Unit,
    ): JPanel {
        val row = JPanel(BorderLayout(10, 0))
        row.isOpaque = false
        row.border = EmptyBorder(5, 0, 5, 0)
        row.add(label("附件 · ${attachment.displayName}", bodyFont, DeepBlue), BorderLayout.CENTER)
        row.add(
            actionButton("打开") { open(attachment.relativePath, attachment.displayName) },
            BorderLayout.EAST,
        )
        return row
    }
}

private class SyncPage(
    private val store: DesktopSyncStore,
    private val server: DesktopSyncServer,
    private val onOpenRecords: (DesktopSection) -> Unit,
) : JPanel(BorderLayout()) {
    init {
        background = WarmBackground
        val content = verticalList()

        val connectionCard = RoundedPanel(SageSoft, SageSoft, 22)
        connectionCard.layout = BoxLayout(connectionCard, BoxLayout.Y_AXIS)
        connectionCard.border = EmptyBorder(20, 22, 20, 22)
        connectionCard.add(label("等待手机连接", headingFont, SageDark).alignLeft())
        connectionCard.add(Box.createVerticalStrut(6))
        connectionCard.add(
            label("请确保电脑和手机连接同一个 Wi-Fi。", bodyFont, DeepBlue).alignLeft(),
        )
        connectionCard.add(Box.createVerticalStrut(18))
        val code = JLabel(server.pairingCode)
        code.font = Font("Consolas", Font.BOLD, 40)
        code.foreground = DeepBlue
        connectionCard.add(code.alignLeft())
        connectionCard.add(label("当前配对码", smallFont, MutedBlue).alignLeft())
        connectionCard.add(Box.createVerticalStrut(16))

        val addresses = server.localAddresses()
        if (addresses.isEmpty()) {
            connectionCard.add(
                label("未检测到 Wi-Fi 地址，请检查网络或防火墙。", bodyFont, Amber)
                    .alignLeft(),
            )
        } else {
            addresses.forEach { address ->
                connectionCard.add(label("手机填写电脑地址：$address", bodyFont, DeepBlue).alignLeft())
            }
        }
        connectionCard.add(label("端口：${server.port}", bodyFont, DeepBlue).alignLeft())
        connectionCard.add(Box.createVerticalStrut(16))
        val buttons = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0))
        buttons.isOpaque = false
        buttons.alignmentX = Component.LEFT_ALIGNMENT
        buttons.add(
            primaryButton("刷新配对码") {
                server.regeneratePairingCode()
                code.text = server.pairingCode
            },
        )
        buttons.add(actionButton("打开数据文件夹") { openDataFolder() })
        connectionCard.add(buttons)
        content.add(connectionCard)
        content.add(Box.createVerticalStrut(14))

        val current = store.current
        val statusCard = RoundedPanel(SurfaceWarm, CardOutline, 22)
        statusCard.layout = BoxLayout(statusCard, BoxLayout.Y_AXIS)
        statusCard.border = EmptyBorder(18, 20, 18, 20)
        statusCard.add(label("电脑上的最新快照", headingFont, DeepBlue).alignLeft())
        statusCard.add(Box.createVerticalStrut(8))
        val dataPathLabel = JLabel(
            "<html><body style='width:620px'>数据目录：" +
                escapeHtml(store.applicationRoot.absolutePath) +
                "</body></html>",
        )
        dataPathLabel.font = smallFont
        dataPathLabel.foreground = MutedBlue
        dataPathLabel.alignmentX = Component.LEFT_ALIGNMENT
        statusCard.add(dataPathLabel)
        statusCard.add(Box.createVerticalStrut(8))
        if (current == null) {
            statusCard.add(label("还没有接收过手机同步。", bodyFont, MutedBlue).alignLeft())
        } else {
            statusCard.add(
                label(
                    "同步时间：${formatDateTime(current.snapshot.exportedAtEpochMillis)}",
                    bodyFont,
                    DeepBlue,
                ).alignLeft(),
            )
            statusCard.add(
                label(
                    "${current.snapshot.taskCount} 个任务卡 · " +
                        "${current.snapshot.contentCardCount} 张内容卡 · " +
                        "${current.snapshot.attachmentCount} 个附件",
                    bodyFont,
                    DeepBlue,
                ).alignLeft(),
            )
            statusCard.add(
                label(
                    "${current.snapshot.learningSnapshots.size} 个学习快照 · " +
                        "${current.snapshot.weekDocuments.size} 份周总结 · " +
                        "${current.snapshot.errorRecords.size} 条错误记录",
                    bodyFont,
                    DeepBlue,
                ).alignLeft(),
            )
            statusCard.add(Box.createVerticalStrut(10))
            statusCard.add(
                actionButton("查看已同步的学习任务") {
                    onOpenRecords(DesktopSection.TASKS)
                }.alignLeft(),
            )
        }
        content.add(statusCard)
        content.add(Box.createVerticalStrut(14))

        val note = JLabel(
            "<html><body style='width:600px'>同步采用一对一电脑接收：" +
                "手机发送本机全部业务数据和附件，电脑端先完整校验，再切换为最新快照。" +
                "旧快照不会被中断的上传覆盖。</body></html>",
        )
        note.font = smallFont
        note.foreground = MutedBlue
        note.alignmentX = Component.LEFT_ALIGNMENT
        content.add(note)
        add(scrollable(content), BorderLayout.CENTER)
    }

    private fun openDataFolder() {
        runCatching {
            store.applicationRoot.mkdirs()
            Desktop.getDesktop().open(store.applicationRoot)
        }.onFailure { error ->
            JOptionPane.showMessageDialog(
                this,
                error.message ?: "无法打开数据文件夹",
                "打开失败",
                JOptionPane.WARNING_MESSAGE,
            )
        }
    }
}

private class WeekStripPanel : JComponent() {
    var snapshot: SyncSnapshot? = null
        set(value) {
            field = value
            repaint()
        }
    var selectedDate: LocalDate = LocalDate.now()
        set(value) {
            field = value
            repaint()
        }
    var onDateSelected: (LocalDate) -> Unit = {}

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                dateAt(event.x)?.let(onDateSelected)
            }
        })
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON,
        )
        val monday = selectedDate.with(
            java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
        )
        val dates = (0L..6L).map(monday::plusDays)
        val cellWidth = width / 7.0
        val counts = snapshot?.tasks
            ?.groupingBy { it.scheduledDate }
            ?.eachCount()
            .orEmpty()

        dates.forEachIndexed { index, date ->
            val x = index * cellWidth
            if (date == selectedDate) {
                g.color = SageSoft
                g.fillRoundRect(
                    (x + 4).toInt(),
                    4,
                    (cellWidth - 8).toInt(),
                    height - 8,
                    14,
                    14,
                )
            }
            if (date == LocalDate.now()) {
                g.color = Sage
                g.stroke = BasicStroke(1.4f)
                g.drawRoundRect(
                    (x + 4).toInt(),
                    4,
                    (cellWidth - 8).toInt(),
                    height - 8,
                    14,
                    14,
                )
            }
            g.font = smallFont
            g.color = MutedBlue
            val weekday = "周${weekdayText(date.dayOfWeek).removePrefix("星期")}"
            val weekdayMetrics = g.fontMetrics
            g.drawString(
                weekday,
                (x + (cellWidth - weekdayMetrics.stringWidth(weekday)) / 2).toInt(),
                19,
            )
            g.font = if (date == selectedDate) {
                Font("Microsoft YaHei UI", Font.BOLD, 18)
            } else {
                Font("Microsoft YaHei UI", Font.PLAIN, 17)
            }
            g.color = DeepBlue
            val day = date.dayOfMonth.toString()
            val dayMetrics = g.fontMetrics
            g.drawString(
                day,
                (x + (cellWidth - dayMetrics.stringWidth(day)) / 2).toInt(),
                height / 2 + dayMetrics.ascent / 2 + 4,
            )
            if (counts.getOrDefault(date, 0) > 0) {
                g.color = Amber
                g.fillOval(
                    (x + cellWidth / 2 - 2).toInt(),
                    height - 13,
                    4,
                    4,
                )
            }
        }
        g.dispose()
    }

    private fun dateAt(px: Int): LocalDate? {
        if (px !in 0 until width) return null
        val monday = selectedDate.with(
            java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
        )
        val index = (px / (width / 7.0)).toInt().coerceIn(0, 6)
        return monday.plusDays(index.toLong())
    }
}

private class CalendarGridPanel : JComponent() {
    var snapshot: SyncSnapshot? = null
        set(value) {
            field = value
            repaint()
        }
    var month: YearMonth = YearMonth.now()
        set(value) {
            field = value
            repaint()
        }
    var selectedDate: LocalDate = LocalDate.now()
        set(value) {
            field = value
            repaint()
        }
    var onDateSelected: (LocalDate) -> Unit = {}

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                val date = dateAt(event.x, event.y) ?: return
                onDateSelected(date)
            }
        })
    }

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON,
        )
        val labels = listOf("一", "二", "三", "四", "五", "六", "日")
        val cellWidth = width / 7.0
        val labelHeight = 30.0
        val cellHeight = (height - labelHeight) / 6.0
        g.font = Font("Microsoft YaHei UI", Font.BOLD, 12)
        g.color = MutedBlue
        labels.forEachIndexed { index, label ->
            val metrics = g.fontMetrics
            val x = index * cellWidth + (cellWidth - metrics.stringWidth(label)) / 2
            g.drawString(label, x.toInt(), 20)
        }

        val counts = snapshot?.tasks
            ?.groupingBy { it.scheduledDate }
            ?.eachCount()
            .orEmpty()
        monthCells().forEachIndexed { index, date ->
            val column = index % 7
            val row = index / 7
            val x = column * cellWidth
            val y = labelHeight + row * cellHeight
            if (date == selectedDate) {
                g.color = SageSoft
                g.fillRoundRect(
                    (x + 3).toInt(),
                    (y + 3).toInt(),
                    (cellWidth - 6).toInt(),
                    (cellHeight - 6).toInt(),
                    14,
                    14,
                )
            }
            if (date == LocalDate.now()) {
                g.color = Sage
                g.stroke = BasicStroke(1.4f)
                g.drawRoundRect(
                    (x + 3).toInt(),
                    (y + 3).toInt(),
                    (cellWidth - 6).toInt(),
                    (cellHeight - 6).toInt(),
                    14,
                    14,
                )
            }
            g.font = if (date == LocalDate.now()) {
                Font("Microsoft YaHei UI", Font.BOLD, 15)
            } else {
                Font("Microsoft YaHei UI", Font.PLAIN, 14)
            }
            g.color = if (YearMonth.from(date) == month) {
                DeepBlue
            } else {
                Color(0xFFA8A49B.toInt())
            }
            val number = date.dayOfMonth.toString()
            val metrics = g.fontMetrics
            g.drawString(
                number,
                (x + (cellWidth - metrics.stringWidth(number)) / 2).toInt(),
                (y + cellHeight / 2 + metrics.ascent / 2).toInt(),
            )
            if (counts.getOrDefault(date, 0) > 0) {
                g.color = Amber
                g.fillOval(
                    (x + cellWidth / 2 - 2).toInt(),
                    (y + cellHeight / 2 + metrics.ascent / 2 + 5).toInt(),
                    4,
                    4,
                )
            }
        }
        g.dispose()
    }

    private fun dateAt(px: Int, py: Int): LocalDate? {
        if (px !in 0 until width || py < 30 || py >= height) return null
        val column = (px / (width / 7.0)).toInt().coerceIn(0, 6)
        val row = ((py - 30) / ((height - 30) / 6.0)).toInt().coerceIn(0, 5)
        return monthCells().getOrNull(row * 7 + column)
    }

    private fun monthCells(): List<LocalDate> {
        val first = month.atDay(1)
        val offset = first.dayOfWeek.value - DayOfWeek.MONDAY.value
        return (0 until 42).map { index -> first.plusDays((index - offset).toLong()) }
    }
}

private class RoundedPanel(
    private val fill: Color,
    private val outline: Color,
    private val radius: Int,
) : JPanel() {
    init {
        isOpaque = false
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics.create() as Graphics2D
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON,
        )
        g.color = fill
        g.fillRoundRect(0, 0, width - 1, height - 1, radius, radius)
        if (outline != fill) {
            g.color = outline
            g.stroke = BasicStroke(1f)
            g.drawRoundRect(0, 0, width - 1, height - 1, radius, radius)
        }
        g.dispose()
        super.paintComponent(graphics)
    }
}

private fun emptyState(title: String, description: String): JPanel {
    val panel = JPanel(GridBagLayout())
    panel.background = WarmBackground
    val card = RoundedPanel(SurfaceWarm, CardOutline, 22)
    card.layout = BoxLayout(card, BoxLayout.Y_AXIS)
    card.border = EmptyBorder(30, 36, 30, 36)
    card.add(label(title, headingFont, DeepBlue))
    card.add(Box.createVerticalStrut(8))
    card.add(label(description, bodyFont, MutedBlue))
    panel.add(card)
    return panel
}

private fun emptyInline(text: String): JLabel = JLabel(text).apply {
    font = bodyFont
    foreground = MutedBlue
    alignmentX = Component.LEFT_ALIGNMENT
    border = EmptyBorder(20, 8, 20, 8)
}

private fun verticalList(): JPanel = JPanel().apply {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    background = WarmBackground
    border = EmptyBorder(2, 2, 14, 2)
}

private fun scrollable(component: JComponent): JScrollPane =
    JScrollPane(component).apply {
        border = BorderFactory.createEmptyBorder()
        viewport.background = WarmBackground
        isOpaque = false
        viewport.isOpaque = true
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        verticalScrollBar.unitIncrement = 18
    }

private fun actionButton(text: String, onClick: () -> Unit): JButton =
    JButton(text).apply {
        font = bodyFont
        foreground = DeepBlue
        background = SurfaceWarm
        isFocusPainted = false
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CardOutline),
            EmptyBorder(8, 14, 8, 14),
        )
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addActionListener { onClick() }
    }

private fun primaryButton(text: String, onClick: () -> Unit): JButton =
    object : JButton(text) {
        override fun paintComponent(graphics: Graphics) {
            val g = graphics.create() as Graphics2D
            g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON,
            )
            g.color = if (model.isPressed) Sidebar else DeepBlue
            g.fillRoundRect(0, 0, width, height, 14, 14)
            g.dispose()
            super.paintComponent(graphics)
        }
    }.apply {
        font = bodyFont
        foreground = Color.WHITE
        isFocusPainted = false
        isContentAreaFilled = false
        isBorderPainted = false
        isOpaque = false
        border = EmptyBorder(9, 16, 9, 16)
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        addActionListener { onClick() }
    }

private fun label(text: String, font: Font, color: Color): JLabel = JLabel(text).apply {
    this.font = font
    foreground = color
}

private fun <T : Component> T.alignLeft(): T {
    (this as? JComponent)?.alignmentX = Component.LEFT_ALIGNMENT
    return this
}

private fun installClick(component: Component, action: () -> Unit) {
    component.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    component.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(event: MouseEvent) {
            action()
        }
    })
    if (component is java.awt.Container) {
        component.components.forEach { child -> installClick(child, action) }
    }
}

private fun multilineLabel(text: String): JTextArea = JTextArea(text).apply {
    font = bodyFont
    foreground = DeepBlue
    isOpaque = false
    isEditable = false
    isFocusable = false
    lineWrap = true
    wrapStyleWord = true
    border = EmptyBorder(3, 0, 3, 0)
    alignmentX = Component.LEFT_ALIGNMENT
    maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
}

private fun pill(text: String): JLabel = JLabel("  $text  ").apply {
    font = smallFont
    foreground = MutedBlue
    background = WarmBackground
    isOpaque = true
    border = EmptyBorder(4, 4, 4, 4)
}

private fun statusPill(text: String, color: Color, background: Color = Color(0xFFFFF4DF.toInt())): JLabel =
    JLabel("  $text  ").apply {
        font = smallFont
        foreground = color
        this.background = background
        isOpaque = true
        border = EmptyBorder(4, 4, 4, 4)
    }

private fun separator(): JComponent = JPanel().apply {
    background = CardOutline
    maximumSize = Dimension(Int.MAX_VALUE, 1)
    preferredSize = Dimension(0, 1)
    alignmentX = Component.LEFT_ALIGNMENT
    border = EmptyBorder(10, 0, 10, 0)
}

private fun masteryText(level: Int): String = when (level) {
    0 -> "看过"
    1 -> "能理解"
    2 -> "不看书能复现"
    3 -> "可做变式习题"
    4 -> "可讲解"
    else -> "未选择"
}

private fun kindText(kind: String): String = when (kind) {
    "AUDIO" -> "录音"
    "FILE" -> "文件"
    else -> "照片"
}

private fun weekdayText(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "星期一"
    DayOfWeek.TUESDAY -> "星期二"
    DayOfWeek.WEDNESDAY -> "星期三"
    DayOfWeek.THURSDAY -> "星期四"
    DayOfWeek.FRIDAY -> "星期五"
    DayOfWeek.SATURDAY -> "星期六"
    DayOfWeek.SUNDAY -> "星期日"
}

private fun formatDateTime(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("yyyy 年 M 月 d 日 HH:mm")
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

private fun escapeHtml(text: String): String =
    text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
