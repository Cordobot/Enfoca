package com.cordobot.enfoca

import com.cordobot.enfoca.domain.model.Habit
import com.cordobot.enfoca.domain.model.PomodoroPhase
import com.cordobot.enfoca.domain.model.PomodoroRules
import com.cordobot.enfoca.domain.model.PomodoroState
import com.cordobot.enfoca.domain.model.Streaks
import com.cordobot.enfoca.domain.repository.HabitRepository
import com.cordobot.enfoca.domain.repository.PomodoroRepository
import com.cordobot.enfoca.domain.repository.ReminderScheduler
import com.cordobot.enfoca.domain.usecase.PomodoroController
import com.cordobot.enfoca.domain.usecase.SaveHabitUseCase
import com.cordobot.enfoca.presentation.focus.formatClock
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class StreaksTest {
    private val today = LocalDate.of(2026, 10, 27)
    private fun days(vararg offsets: Long) = offsets.map { today.minusDays(it) }.toSet()

    @Test
    fun `streak counts back from today`() = assertEquals(3, Streaks.current(days(0, 1, 2, 4), today))

    @Test
    fun `pending today does not break yesterday's streak`() = assertEquals(2, Streaks.current(days(1, 2), today))

    @Test
    fun `missing yesterday breaks the streak`() = assertEquals(0, Streaks.current(days(2, 3), today))

    @Test
    fun `best streak is the longest run`() = assertEquals(4, Streaks.best(days(0, 2, 3, 4, 5, 8)))

    @Test
    fun `last week ends today`() =
        assertEquals(listOf(false, false, false, false, true, false, true), Streaks.lastDays(days(0, 2), today))
}

class PomodoroRulesTest {

    @Test
    fun `pause keeps the remaining time and start resumes from it`() {
        val running = PomodoroRules.start(PomodoroState(), now = 0)
        val paused = PomodoroRules.pause(running, now = 60_000)

        assertEquals(24 * 60_000L, paused.pausedRemainingMillis)
        assertEquals(1_000L + 24 * 60_000L, PomodoroRules.start(paused, now = 1_000).endsAtMillis)
    }

    @Test
    fun `every fourth focus session is followed by a long break`() {
        var state = PomodoroState()
        val phases = mutableListOf<PomodoroPhase>()
        repeat(8) {
            state = PomodoroRules.advance(state)
            phases += state.phase
        }
        assertEquals(
            listOf(
                PomodoroPhase.SHORT_BREAK, PomodoroPhase.FOCUS, PomodoroPhase.SHORT_BREAK, PomodoroPhase.FOCUS,
                PomodoroPhase.SHORT_BREAK, PomodoroPhase.FOCUS, PomodoroPhase.LONG_BREAK, PomodoroPhase.FOCUS
            ),
            phases
        )
        assertEquals(4, state.completedFocusSessions)
    }

    @Test
    fun `clock rounds up seconds`() {
        assertEquals("25:00", formatClock(25 * 60_000L))
        assertEquals("00:01", formatClock(1))
        assertEquals("00:00", formatClock(0))
    }
}

class UseCaseTest {
    private val habits = mockk<HabitRepository>()
    private val scheduler = mockk<ReminderScheduler>(relaxed = true)

    private fun habit(name: String, reminder: LocalTime?) =
        Habit(name = name, emoji = "💧", color = 0xFF000000, reminder = reminder, createdOn = LocalDate.of(2026, 1, 1))

    @Test
    fun `saving a habit with reminder schedules it with the new id`() = runTest {
        val saved = slot<Habit>()
        coEvery { habits.save(any()) } returns 7
        every { scheduler.scheduleHabitReminder(capture(saved)) } returns Unit

        val result = SaveHabitUseCase(habits, scheduler)(habit("  Leer  ", LocalTime.of(21, 30)))

        assertEquals(7L, result.getOrNull())
        assertEquals(7L, saved.captured.id)
        assertEquals("Leer", saved.captured.name)
    }

    @Test
    fun `habit without reminder cancels any previous one`() = runTest {
        coEvery { habits.save(any()) } returns 3

        SaveHabitUseCase(habits, scheduler)(habit("Correr", reminder = null))

        verify { scheduler.cancelHabitReminder(3) }
        verify(exactly = 0) { scheduler.scheduleHabitReminder(any()) }
    }

    @Test
    fun `blank name is rejected without touching the database`() = runTest {
        val result = SaveHabitUseCase(habits, scheduler)(habit("   ", null))

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { habits.save(any()) }
    }

    @Test
    fun `starting the timer schedules the alarm at the end of the phase`() = runTest {
        val repository = mockk<PomodoroRepository>(relaxed = true)
        every { repository.state } returns flowOf(PomodoroState())

        PomodoroController(repository, scheduler).start(now = 1_000)

        verify { scheduler.schedulePhaseEnd(1_000 + PomodoroPhase.FOCUS.durationMillis, PomodoroPhase.FOCUS) }
        coVerify { repository.save(match { it.isRunning }) }
    }

    @Test
    fun `pausing cancels the alarm`() = runTest {
        val repository = mockk<PomodoroRepository>(relaxed = true)
        every { repository.state } returns flowOf(PomodoroRules.start(PomodoroState(), 0))

        PomodoroController(repository, scheduler).pause(now = 5_000)

        verify { scheduler.cancelPhaseEnd() }
    }
}
