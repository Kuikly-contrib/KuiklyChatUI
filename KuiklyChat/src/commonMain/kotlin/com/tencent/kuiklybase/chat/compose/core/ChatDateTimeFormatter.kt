package com.tencent.kuiklybase.chat.compose.core

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * Formats message timestamps for display. Implementations may use device locale, a fixed
 * business time zone, or UTC. [ChatMessage.createdAt] is always epoch millis (UTC instant).
 */
interface ChatDateTimeFormatter {
    fun formatMessageTime(timestampMillis: Long, pattern: String = "HH:mm"): String

    /**
     * Label for a date separator between [previousCreatedAt] and [currentCreatedAt], or `null`
     * when no separator should appear (same calendar day, or invalid current time).
     */
    fun dateSeparatorLabel(
        previousCreatedAt: Long?,
        currentCreatedAt: Long,
        nowMillis: Long,
        strings: ChatStrings,
    ): String?
}

/** UTC display; matches legacy [com.tencent.kuiklybase.chat.compose.ui.formatMessageTime] behavior. */
object UtcChatDateTimeFormatter : ChatDateTimeFormatter {
    private val timeZone = TimeZone.UTC

    override fun formatMessageTime(timestampMillis: Long, pattern: String): String {
        if (timestampMillis <= 0) return ""
        if (pattern != "HH:mm") {
            return formatMessageTime(timestampMillis, "HH:mm")
        }
        val dateTime = Instant.fromEpochMilliseconds(timestampMillis).toLocalDateTime(timeZone)
        return formatHourMinute(dateTime.hour, dateTime.minute)
    }

    override fun dateSeparatorLabel(
        previousCreatedAt: Long?,
        currentCreatedAt: Long,
        nowMillis: Long,
        strings: ChatStrings,
    ): String? = dateSeparatorLabelInZone(
        previousCreatedAt = previousCreatedAt,
        currentCreatedAt = currentCreatedAt,
        nowMillis = nowMillis,
        strings = strings,
        timeZone = timeZone,
    )
}

/**
 * Formats using [TimeZone.currentSystemDefault] by default. Hosts may call [withTimeZone] for
 * fixed business zones or tests.
 */
class LocalChatDateTimeFormatter private constructor(
    private val timeZone: TimeZone,
) : ChatDateTimeFormatter {
    override fun formatMessageTime(timestampMillis: Long, pattern: String): String {
        if (timestampMillis <= 0) return ""
        if (pattern != "HH:mm") {
            return formatMessageTime(timestampMillis, "HH:mm")
        }
        val dateTime = Instant.fromEpochMilliseconds(timestampMillis).toLocalDateTime(timeZone)
        return formatHourMinute(dateTime.hour, dateTime.minute)
    }

    override fun dateSeparatorLabel(
        previousCreatedAt: Long?,
        currentCreatedAt: Long,
        nowMillis: Long,
        strings: ChatStrings,
    ): String? = dateSeparatorLabelInZone(
        previousCreatedAt = previousCreatedAt,
        currentCreatedAt = currentCreatedAt,
        nowMillis = nowMillis,
        strings = strings,
        timeZone = timeZone,
    )

    companion object {
        val Default: LocalChatDateTimeFormatter =
            LocalChatDateTimeFormatter(TimeZone.currentSystemDefault())

        fun withTimeZone(timeZoneId: String): LocalChatDateTimeFormatter =
            LocalChatDateTimeFormatter(TimeZone.of(timeZoneId))
    }
}

internal fun formatHourMinute(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

internal fun dateSeparatorLabelInZone(
    previousCreatedAt: Long?,
    currentCreatedAt: Long,
    nowMillis: Long,
    strings: ChatStrings,
    timeZone: TimeZone,
): String? {
    if (currentCreatedAt <= 0) return null
    val currentDate = Instant.fromEpochMilliseconds(currentCreatedAt)
        .toLocalDateTime(timeZone)
        .date
    if (previousCreatedAt != null && previousCreatedAt > 0) {
        val previousDate = Instant.fromEpochMilliseconds(previousCreatedAt)
            .toLocalDateTime(timeZone)
            .date
        if (previousDate == currentDate) return null
    }
    val timeText = formatHourMinute(
        Instant.fromEpochMilliseconds(currentCreatedAt).toLocalDateTime(timeZone).hour,
        Instant.fromEpochMilliseconds(currentCreatedAt).toLocalDateTime(timeZone).minute,
    )
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
    return when (currentDate) {
        today -> "${strings.today} $timeText"
        today.minus(1, DateTimeUnit.DAY) -> "${strings.yesterday} $timeText"
        else -> formatDateSeparatorFallback(currentDate, today.year, timeText)
    }
}

private fun formatDateSeparatorFallback(
    date: LocalDate,
    currentYear: Int,
    timeText: String,
): String {
    val month = date.monthNumber.toString().padStart(2, '0')
    val day = date.dayOfMonth.toString().padStart(2, '0')
    return if (date.year == currentYear) {
        "$month-$day $timeText"
    } else {
        "${date.year}-$month-$day $timeText"
    }
}
