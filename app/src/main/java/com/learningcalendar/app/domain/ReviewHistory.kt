package com.learningcalendar.app.domain

import java.time.LocalDate

data class ReviewHistoryGroup(
    val day: Int,
    val tasks: List<StudyTask>,
)

object ReviewHistoryLogic {
    fun availableGroups(
        tasks: List<StudyTask>,
        today: LocalDate,
    ): List<ReviewHistoryGroup> {
        val source = tasks.firstOrNull {
            it.sourceType == TaskSourceType.ORIGINAL
        } ?: return emptyList()
        val reviews = tasks
            .filter { it.sourceType == TaskSourceType.AUTO_REVIEW }
            .sortedBy { it.reviewSequence ?: Int.MAX_VALUE }
        val reached = reviews.filter { !it.scheduledDate.isAfter(today) }

        return buildList {
            add(ReviewHistoryGroup(day = 1, tasks = listOf(source)))
            listOf(2, 3, 7).forEach { day ->
                val reachedForDay = reached.filter {
                    (it.reviewSequence ?: Int.MAX_VALUE) <= day
                }
                if (
                    reviews.any {
                        it.reviewSequence == day && !it.scheduledDate.isAfter(today)
                    }
                ) {
                    add(
                        ReviewHistoryGroup(
                            day = day,
                            tasks = listOf(source) + reachedForDay,
                        ),
                    )
                }
            }
        }
    }
}
