package com.learningcalendar.app.domain

data class CompletionSummary(
    val completed: Int,
    val total: Int,
) {
    val rate: Int
        get() = if (total == 0) 0 else completed * 100 / total
}

object DailyOverviewLogic {
    fun newTasks(tasks: List<StudyTask>): List<StudyTask> =
        tasks.filter { it.section == TaskSection.NEW }

    fun reviewTasks(tasks: List<StudyTask>): List<StudyTask> =
        tasks.filter { it.section == TaskSection.REVIEW }

    fun summary(tasks: List<StudyTask>): CompletionSummary =
        CompletionSummary(
            completed = tasks.count(StudyTask::isCompleted),
            total = tasks.size,
        )
}
