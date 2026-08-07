package com.tencent.kuiklybase.chat.compose.core

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatDateTimeFormatterTest {
  private val strings = ChatStrings.Default
  private val utc = LocalChatDateTimeFormatter.withTimeZone("UTC")

  @Test
  fun formatMessageTime_returnsEmptyForZeroTimestamp() {
    assertEquals("", UtcChatDateTimeFormatter.formatMessageTime(0L))
    assertEquals("", utc.formatMessageTime(-1L))
  }

  @Test
  fun utcFormatter_formatsHourMinute() {
    val timestamp = instantUtc(1970, 1, 1, 14, 30)
    assertEquals("14:30", UtcChatDateTimeFormatter.formatMessageTime(timestamp))
  }

  @Test
  fun localFormatter_respectsFixedOffset() {
    val timestamp = instantUtc(2024, 6, 15, 14, 30)
    assertEquals("14:30", utc.formatMessageTime(timestamp))
    val eastEight = LocalChatDateTimeFormatter.withTimeZone("+08:00")
    assertEquals("22:30", eastEight.formatMessageTime(timestamp))
  }

  @Test
  fun dateSeparator_sameCalendarDay_returnsNull() {
    val morning = instantUtc(2024, 6, 15, 9, 0)
    val noon = instantUtc(2024, 6, 15, 12, 0)
    assertNull(
      utc.dateSeparatorLabel(
        previousCreatedAt = morning,
        currentCreatedAt = noon,
        nowMillis = noon,
        strings = strings,
      ),
    )
  }

  @Test
  fun dateSeparator_today_showsTodayLabel() {
    val yesterday = instantUtc(2024, 6, 14, 23, 0)
    val todayMorning = instantUtc(2024, 6, 15, 9, 15)
    val now = instantUtc(2024, 6, 15, 12, 0)
    assertEquals(
      "今天 09:15",
      utc.dateSeparatorLabel(
        previousCreatedAt = yesterday,
        currentCreatedAt = todayMorning,
        nowMillis = now,
        strings = strings,
      ),
    )
  }

  @Test
  fun dateSeparator_yesterday_showsYesterdayLabel() {
    val twoDaysAgo = instantUtc(2024, 6, 13, 10, 0)
    val yesterday = instantUtc(2024, 6, 14, 8, 45)
    val now = instantUtc(2024, 6, 15, 12, 0)
    assertEquals(
      "昨天 08:45",
      utc.dateSeparatorLabel(
        previousCreatedAt = twoDaysAgo,
        currentCreatedAt = yesterday,
        nowMillis = now,
        strings = strings,
      ),
    )
  }

  @Test
  fun dateSeparator_olderSameYear_showsMonthDay() {
    val previous = instantUtc(2024, 5, 1, 10, 0)
    val current = instantUtc(2024, 6, 1, 11, 30)
    val now = instantUtc(2024, 6, 15, 12, 0)
    assertEquals(
      "06-01 11:30",
      utc.dateSeparatorLabel(
        previousCreatedAt = previous,
        currentCreatedAt = current,
        nowMillis = now,
        strings = strings,
      ),
    )
  }

  @Test
  fun dateSeparator_previousYear_showsFullDate() {
    val previous = instantUtc(2023, 5, 1, 10, 0)
    val current = instantUtc(2023, 6, 1, 9, 0)
    val now = instantUtc(2024, 6, 15, 12, 0)
    assertEquals(
      "2023-06-01 09:00",
      utc.dateSeparatorLabel(
        previousCreatedAt = previous,
        currentCreatedAt = current,
        nowMillis = now,
        strings = strings,
      ),
    )
  }

  private fun instantUtc(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    LocalDateTime(year, month, day, hour, minute)
      .toInstant(TimeZone.UTC)
      .toEpochMilliseconds()
}
