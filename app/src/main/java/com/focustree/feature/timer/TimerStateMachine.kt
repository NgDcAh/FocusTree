package com.focustree.feature.timer

import com.focustree.core.time.Clock

private const val MILLIS_PER_MINUTE = 60_000L

enum class SessionOutcome {
    ACTIVE,
    COMPLETED,
    ABANDONED,
}

data class FocusSession(
    val id: String,
    val intention: String,
    val plannedDurationMillis: Long,
    val startedAtEpochMillis: Long,
    val expectedEndAtEpochMillis: Long,
    val finishedAtEpochMillis: Long? = null,
    val outcome: SessionOutcome = SessionOutcome.ACTIVE,
    val rewardGranted: Boolean = false,
)

data class TimerSnapshot(
    val session: FocusSession,
    val remainingMillis: Long,
    val progress: Float,
)

class TimerStateMachine(
    private val clock: Clock,
    restoredSession: FocusSession? = null,
) {
    var session: FocusSession? = restoredSession
        private set

    fun start(
        id: String,
        setup: SessionSetup,
    ): FocusSession {
        check(session?.outcome != SessionOutcome.ACTIVE) { "A session is already active" }
        check(SessionSetupValidator.validate(setup.intention, setup.durationMinutes).isEmpty()) {
            "Session setup is invalid"
        }

        val startedAt = clock.nowEpochMillis()
        val durationMillis = setup.durationMinutes * MILLIS_PER_MINUTE
        return FocusSession(
            id = id,
            intention = setup.intention,
            plannedDurationMillis = durationMillis,
            startedAtEpochMillis = startedAt,
            expectedEndAtEpochMillis = saturatedAdd(startedAt, durationMillis),
        ).also { session = it }
    }

    fun snapshot(): TimerSnapshot? {
        reconcileCompletion()
        val current = session ?: return null
        val remaining = remainingMillis(current, clock.nowEpochMillis())
        val elapsed = current.plannedDurationMillis - remaining
        val progress =
            if (current.plannedDurationMillis == 0L) {
                1f
            } else {
                elapsed.toFloat() / current.plannedDurationMillis
            }
        return TimerSnapshot(current, remaining, progress.coerceIn(0f, 1f))
    }

    fun complete(): FocusSession? = finish(SessionOutcome.COMPLETED)

    fun abandon(): FocusSession? = finish(SessionOutcome.ABANDONED)

    private fun finish(outcome: SessionOutcome): FocusSession? =
        session
            ?.let { current ->
                if (current.outcome == SessionOutcome.ACTIVE) {
                    current.copy(
                        finishedAtEpochMillis = maxOf(clock.nowEpochMillis(), current.startedAtEpochMillis),
                        outcome = outcome,
                        rewardGranted = outcome == SessionOutcome.COMPLETED,
                    )
                } else {
                    current
                }
            }?.also { session = it }

    private fun reconcileCompletion() {
        val current = session ?: return
        if (current.outcome == SessionOutcome.ACTIVE && remainingMillis(current, clock.nowEpochMillis()) == 0L) {
            complete()
        }
    }

    private fun remainingMillis(
        current: FocusSession,
        nowEpochMillis: Long,
    ): Long =
        when {
            current.outcome != SessionOutcome.ACTIVE -> {
                0L
            }

            nowEpochMillis <= current.startedAtEpochMillis -> {
                current.plannedDurationMillis
            }

            nowEpochMillis >= current.expectedEndAtEpochMillis -> {
                0L
            }

            else -> {
                (current.expectedEndAtEpochMillis - nowEpochMillis)
                    .coerceAtMost(current.plannedDurationMillis)
            }
        }

    private fun saturatedAdd(
        value: Long,
        positiveDelta: Long,
    ): Long =
        if (value > Long.MAX_VALUE - positiveDelta) {
            Long.MAX_VALUE
        } else {
            value + positiveDelta
        }
}
