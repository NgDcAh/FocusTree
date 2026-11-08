package com.focustree.feature.timer

import com.focustree.core.time.Clock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TimerStateMachineTest {
    @Test
    fun `remaining time is derived from timestamps and completes at zero`() {
        val clock = FakeClock(1_000L)
        val timer = TimerStateMachine(clock)
        timer.start("one", SessionSetup("Write", 5))

        clock.advanceBy(90_000L)
        assertEquals(210_000L, timer.snapshot()?.remainingMillis)

        clock.advanceBy(210_000L)
        val completed = timer.snapshot()?.session
        assertEquals(0L, timer.snapshot()?.remainingMillis)
        assertEquals(SessionOutcome.COMPLETED, completed?.outcome)
        assertTrue(completed?.rewardGranted == true)
    }

    @Test
    fun `second active session is rejected`() {
        val timer = TimerStateMachine(FakeClock(0L))
        timer.start("one", SessionSetup("", 15))

        try {
            timer.start("two", SessionSetup("", 25))
            fail("Expected a second active session to be rejected")
        } catch (_: IllegalStateException) {
            assertEquals("one", timer.session?.id)
        }
    }

    @Test
    fun `completion and abandonment are idempotent terminal transitions`() {
        val clock = FakeClock(10L)
        val completedTimer = TimerStateMachine(clock)
        completedTimer.start("complete", SessionSetup("", 15))
        val completed = completedTimer.complete()
        clock.advanceBy(1_000L)
        assertSame(completed, completedTimer.complete())
        assertSame(completed, completedTimer.abandon())
        assertTrue(completed?.rewardGranted == true)

        val abandonedTimer = TimerStateMachine(clock)
        abandonedTimer.start("abandon", SessionSetup("", 15))
        val abandoned = abandonedTimer.abandon()
        assertSame(abandoned, abandonedTimer.abandon())
        assertSame(abandoned, abandonedTimer.complete())
        assertFalse(abandoned?.rewardGranted == true)
    }

    @Test
    fun `clock jumps clamp remaining time and never duplicate reward`() {
        val clock = FakeClock(100_000L)
        val timer = TimerStateMachine(clock)
        timer.start("jump", SessionSetup("", 5))

        clock.now = -1_000_000L
        assertEquals(300_000L, timer.snapshot()?.remainingMillis)

        clock.now = Long.MAX_VALUE
        val first = timer.snapshot()?.session
        val second = timer.snapshot()?.session
        assertEquals(0L, timer.snapshot()?.remainingMillis)
        assertSame(first, second)
        assertTrue(first?.rewardGranted == true)
    }

    @Test
    fun `restoration reconciles persisted timestamps without waiting`() {
        val persisted =
            FocusSession(
                id = "restored",
                intention = "Read",
                plannedDurationMillis = 300_000L,
                startedAtEpochMillis = 1_000L,
                expectedEndAtEpochMillis = 301_000L,
            )
        val activeTimer = TimerStateMachine(FakeClock(121_000L), persisted)
        assertEquals(180_000L, activeTimer.snapshot()?.remainingMillis)
        assertEquals(SessionOutcome.ACTIVE, activeTimer.session?.outcome)

        val expiredTimer = TimerStateMachine(FakeClock(400_000L), persisted)
        assertEquals(SessionOutcome.COMPLETED, expiredTimer.snapshot()?.session?.outcome)
        assertTrue(expiredTimer.session?.rewardGranted == true)
    }

    private class FakeClock(
        var now: Long,
    ) : Clock {
        override fun nowEpochMillis(): Long = now

        fun advanceBy(millis: Long) {
            now += millis
        }
    }
}
