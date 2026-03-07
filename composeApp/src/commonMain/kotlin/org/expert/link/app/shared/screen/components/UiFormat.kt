package org.expert.link.app.shared.screen.components

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

fun initials(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return "?"
    val parts = trimmed.split(' ').filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        else -> trimmed.take(2).uppercase()
    }
}

fun formatChatListTime(instant: Instant): String {
    val zone = TimeZone.currentSystemDefault()
    val local = instant.toLocalDateTime(zone)
    val now = Clock.System.now().toLocalDateTime(zone)
    val days = local.date.daysUntil(now.date)
    return when {
        days <= 0 -> "%02d:%02d".format(local.hour, local.minute)
        days == 1 -> "Вчера"
        days < 7 -> local.dayOfWeek.name.lowercase().replaceFirstChar(Char::titlecase)
        else -> "%02d.%02d".format(local.dayOfMonth, local.monthNumber)
    }
}

fun formatMessageTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d:%02d".format(local.hour, local.minute)
}
