package com.cordobot.enfoca

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cordobot.enfoca.domain.model.Habit
import com.cordobot.enfoca.domain.model.HabitProgress
import com.cordobot.enfoca.presentation.habits.HabitCard
import com.cordobot.enfoca.presentation.theme.EnfocaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class HabitCardTest {

    @get:Rule
    val compose = createComposeRule()

    private val habit = Habit(1, "Leer 20 minutos", "📚", 0xFFE76F51, null, LocalDate.of(2026, 1, 1))

    @Test
    fun showsStreakAndTogglesCompletion() {
        var toggles = 0
        compose.setContent {
            EnfocaTheme {
                var done by remember { mutableStateOf(false) }
                HabitCard(
                    progress = HabitProgress(habit, done, currentStreak = if (done) 4 else 3, bestStreak = 9, lastWeek = List(7) { it > 3 }),
                    onToggle = { toggles++; done = !done },
                    onClick = {}
                )
            }
        }

        compose.onNodeWithText("Leer 20 minutos").assertIsDisplayed()
        compose.onNodeWithText("🔥 3 días · mejor 9").assertIsDisplayed()

        compose.onNodeWithContentDescription("Completar Leer 20 minutos").performClick()

        compose.onNodeWithText("🔥 4 días · mejor 9").assertIsDisplayed()
        compose.onNodeWithContentDescription("Desmarcar Leer 20 minutos").assertIsDisplayed()
        assertEquals(1, toggles)
    }

    @Test
    fun habitWithoutStreakInvitesToStart() {
        compose.setContent {
            EnfocaTheme {
                HabitCard(HabitProgress(habit, false, 0, 0, List(7) { false }), onToggle = {}, onClick = {})
            }
        }
        compose.onNodeWithText("Empieza tu racha hoy").assertIsDisplayed()
    }
}
