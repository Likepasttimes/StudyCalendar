package com.learningcalendar.app.domain

import java.time.LocalDate

data class ReviewMilestone(
    val sequence: Int,
    val date: LocalDate,
)

object ReviewSchedule {
    val reviewSequences: List<Int> = listOf(2, 3, 7)

    fun milestones(createdDate: LocalDate): List<ReviewMilestone> =
        reviewSequences.map { sequence ->
            ReviewMilestone(
                sequence = sequence,
                date = createdDate.plusDays((sequence - 1).toLong()),
            )
        }
}
