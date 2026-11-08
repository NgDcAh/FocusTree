package com.focustree.feature.garden

import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GardenProjectionTest {
    @Test
    fun completedSessionsMapToOneGrowthEachAndAbandonedSessionsAreExcluded() {
        val window = GardenDayWindow(1_000L, 2_000L)
        val records =
            listOf(
                session("completed-1", SessionOutcome.COMPLETED, 1_500L),
                session("completed-2", SessionOutcome.COMPLETED, 1_600L),
                session("abandoned", SessionOutcome.ABANDONED, 1_700L),
                session("yesterday", SessionOutcome.COMPLETED, 999L),
                session("tomorrow", SessionOutcome.COMPLETED, 2_000L),
                session("unfinished", SessionOutcome.COMPLETED, null),
                session("completed-1", SessionOutcome.COMPLETED, 1_500L),
            )

        val growth = GardenProjection.projectToday(records, window)

        assertEquals(listOf("completed-1", "completed-2"), growth.map(GardenGrowth::sessionId))
    }

    @Test
    fun dayWindowUsesLocalMidnightAcrossDaylightSavingChanges() {
        val zone = TimeZone.getTimeZone("America/Los_Angeles")
        val springNoon = localMillis(zone, 2026, Calendar.MARCH, 8)
        val fallNoon = localMillis(zone, 2026, Calendar.NOVEMBER, 1)

        val springWindow = GardenProjection.todayWindow(springNoon, zone)
        val fallWindow = GardenProjection.todayWindow(fallNoon, zone)

        assertEquals(23L * MILLIS_PER_HOUR, springWindow.endEpochMillis - springWindow.startEpochMillis)
        assertEquals(25L * MILLIS_PER_HOUR, fallWindow.endEpochMillis - fallWindow.startEpochMillis)
    }

    private fun localMillis(
        zone: TimeZone,
        year: Int,
        month: Int,
        day: Int,
    ): Long =
        Calendar
            .getInstance(zone)
            .apply {
                set(year, month, day, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

    private fun session(
        id: String,
        outcome: SessionOutcome,
        finishedAt: Long?,
    ) = FocusSession(
        id = id,
        intention = id,
        plannedDurationMillis = MILLIS_PER_HOUR,
        startedAtEpochMillis = finishedAt ?: 0L,
        expectedEndAtEpochMillis = finishedAt ?: 0L,
        finishedAtEpochMillis = finishedAt,
        outcome = outcome,
        rewardGranted = outcome == SessionOutcome.COMPLETED,
    )

    private companion object {
        const val MILLIS_PER_HOUR = 3_600_000L
    }
}
