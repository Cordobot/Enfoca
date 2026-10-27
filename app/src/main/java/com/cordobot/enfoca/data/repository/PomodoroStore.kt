package com.cordobot.enfoca.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cordobot.enfoca.domain.model.PomodoroPhase
import com.cordobot.enfoca.domain.model.PomodoroState
import com.cordobot.enfoca.domain.repository.PomodoroRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pomodoroStore: DataStore<Preferences> by preferencesDataStore(name = "pomodoro")

/** Keeps the timer in DataStore so a running session survives process death. */
@Singleton
class PomodoroStore @Inject constructor(
    @ApplicationContext private val context: Context
) : PomodoroRepository {

    private val phaseKey = stringPreferencesKey("phase")
    private val endsAtKey = longPreferencesKey("ends_at")
    private val pausedKey = longPreferencesKey("paused_remaining")
    private val completedKey = intPreferencesKey("completed_focus")

    override val state: Flow<PomodoroState> = context.pomodoroStore.data.map { prefs ->
        val phase = prefs[phaseKey]?.let { runCatching { PomodoroPhase.valueOf(it) }.getOrNull() } ?: PomodoroPhase.FOCUS
        PomodoroState(
            phase = phase,
            endsAtMillis = prefs[endsAtKey],
            pausedRemainingMillis = prefs[pausedKey] ?: phase.durationMillis,
            completedFocusSessions = prefs[completedKey] ?: 0
        )
    }

    override suspend fun save(state: PomodoroState) {
        context.pomodoroStore.edit { prefs ->
            prefs[phaseKey] = state.phase.name
            if (state.endsAtMillis != null) prefs[endsAtKey] = state.endsAtMillis else prefs.remove(endsAtKey)
            prefs[pausedKey] = state.pausedRemainingMillis
            prefs[completedKey] = state.completedFocusSessions
        }
    }
}
