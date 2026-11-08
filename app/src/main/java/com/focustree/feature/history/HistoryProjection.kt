package com.focustree.feature.history

import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class HistoryDayWindow(
    val startEpochMillis: Long,
    val endEpochMillis: Long,
)

data class HistorySummary(
    val sessions: List<FocusSession>,
    val todayCompletedMillis: Long,
    val sevenDayCompletedMillis: List<Long>,
    val sevenDayLabels: List<String>,
)

object HistoryProjection {
    fun dayWindow(
        dayOffset: Int,
        nowEpochMillis: Long,
        timeZone: TimeZone,
    ): HistoryDayWindow {
        val start =
            Calendar.getInstance(timeZone).apply {
                timeInMillis = nowEpochMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, dayOffset)
            }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        return HistoryDayWindow(start.timeInMillis, end.timeInMillis)
    }

    fun summarize(
        sessions: List<FocusSession>,
        nowEpochMillis: Long,
        timeZone: TimeZone,
    ): HistorySummary {
        val unique = sessions.distinctBy(FocusSession::id)
        val dailyTotals =
            (6 downTo 0).map { offset ->
                val window = dayWindow(-offset, nowEpochMillis, timeZone)
                unique
                    .asSequence()
                    .filter { it.outcome == SessionOutcome.COMPLETED }
                    .filter {
                        it.finishedAtEpochMillis?.let { time ->
                            time >= window.startEpochMillis && time < window.endEpochMillis
                        } == true
                    }.sumOf(::completedFocusDurationMillis)
            }
        val labels =
            (6 downTo 0).map { offset ->
                val day =
                    Calendar.getInstance(timeZone).apply {
                        timeInMillis = nowEpochMillis
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_MONTH, -offset)
                    }
                DateFormat
                    .getDateInstance(DateFormat.SHORT)
                    .apply { setTimeZone(timeZone) }
                    .format(Date(day.timeInMillis))
            }
        return HistorySummary(
            sessions =
                unique.sortedWith(
                    compareByDescending<FocusSession> { it.startedAtEpochMillis }.thenByDescending { it.id },
                ),
            todayCompletedMillis = dailyTotals.last(),
            sevenDayCompletedMillis = dailyTotals,
            sevenDayLabels = labels,
        )
    }

    private fun completedFocusDurationMillis(session: FocusSession): Long {
        val finishedAt = session.finishedAtEpochMillis ?: return 0L
        val actualDuration = (finishedAt - session.startedAtEpochMillis).coerceAtLeast(0L)
        return minOf(actualDuration, session.plannedDurationMillis.coerceAtLeast(0L))
    }
}
