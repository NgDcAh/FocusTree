package com.focustree.feature.history

import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

class HistoryProjectionTest {
    @Test
    fun `history sorts newest first and totals only completed sessions`() {
        val zone = TimeZone.getTimeZone("UTC")
        val today =
            LocalDate
                .of(2026, 11, 7)
                .atTime(12, 0)
                .atZone(ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        val result =
            HistoryProjection.summarize(
                sessions =
                    listOf(
                        session("old", 1, SessionOutcome.COMPLETED, today - DAY_MILLIS),
                        session("abandoned", 3, SessionOutcome.ABANDONED, today),
                        session("new", 2, SessionOutcome.COMPLETED, today),
                        session("new", 9, SessionOutcome.COMPLETED, today),
                    ),
                nowEpochMillis = today,
                timeZone = zone,
            )
        assertEquals(listOf("new", "abandoned", "old"), result.sessions.map { it.id })
        assertEquals(2 * MINUTE_MILLIS, result.todayCompletedMillis)
        assertEquals(3 * MINUTE_MILLIS, result.sevenDayCompletedMillis.sum())
    }

    @Test
    fun `seven day local buckets follow spring and autumn daylight saving boundaries`() {
        val zone = TimeZone.getTimeZone("America/New_York")
        val spring =
            LocalDate
                .of(2026, 3, 8)
                .atTime(12, 0)
                .atZone(ZoneId.of("America/New_York"))
                .toInstant()
                .toEpochMilli()
        val autumn =
            LocalDate
                .of(2026, 11, 1)
                .atTime(12, 0)
                .atZone(ZoneId.of("America/New_York"))
                .toInstant()
                .toEpochMilli()
        val springWindow = HistoryProjection.dayWindow(0, spring, zone)
        val autumnWindow = HistoryProjection.dayWindow(0, autumn, zone)
        assertEquals(23 * 60 * MINUTE_MILLIS, springWindow.endEpochMillis - springWindow.startEpochMillis)
        assertEquals(25 * 60 * MINUTE_MILLIS, autumnWindow.endEpochMillis - autumnWindow.startEpochMillis)
        val result =
            HistoryProjection.summarize(
                listOf(
                    session("before", 5, SessionOutcome.COMPLETED, springWindow.startEpochMillis - 1),
                    session("at-start", 7, SessionOutcome.COMPLETED, springWindow.startEpochMillis),
                ),
                spring,
                zone,
            )
        assertEquals(7 * MINUTE_MILLIS, result.todayCompletedMillis)
    }

    private fun session(
        id: String,
        minutes: Int,
        outcome: SessionOutcome,
        finished: Long,
    ) = FocusSession(
        id,
        id,
        minutes * MINUTE_MILLIS,
        finished - minutes * MINUTE_MILLIS,
        finished,
        finished,
        outcome,
        outcome == SessionOutcome.COMPLETED,
    )

    private companion object {
        const val MINUTE_MILLIS = 60_000L
        const val DAY_MILLIS = 86_400_000L
    }
}
