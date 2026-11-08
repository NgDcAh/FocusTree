package com.focustree.feature.garden

import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import java.util.Calendar
import java.util.TimeZone

data class GardenDayWindow(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
)

data class GardenGrowth(
    val sessionId: String,
    val intention: String,
    val plannedDurationMillis: Long,
)

object GardenProjection {
    fun todayWindow(
        nowEpochMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): GardenDayWindow {
        val start =
            Calendar.getInstance(timeZone).apply {
                timeInMillis = nowEpochMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        return GardenDayWindow(start.timeInMillis, end.timeInMillis)
    }

    fun projectToday(
        sessions: List<FocusSession>,
        window: GardenDayWindow,
    ): List<GardenGrowth> =
        sessions
            .asSequence()
            .filter { session -> session.outcome == SessionOutcome.COMPLETED }
            .filter { session ->
                val finishedAt = session.finishedAtEpochMillis ?: return@filter false
                finishedAt >= window.startEpochMillis && finishedAt < window.endEpochMillis
            }.distinctBy(FocusSession::id)
            .map { session ->
                GardenGrowth(
                    sessionId = session.id,
                    intention = session.intention,
                    plannedDurationMillis = session.plannedDurationMillis,
                )
            }.toList()
}
