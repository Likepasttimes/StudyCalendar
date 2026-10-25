package com.learningcalendar.app.ui

data class UiFeedback(
    val text: String,
    val isError: Boolean = false,
)

fun Throwable.toChineseUserMessage(): String =
    message?.takeIf { text -> text.any { character -> character in '\u4e00'..'\u9fff' } }
        ?: "保存失败，请稍后重试"
