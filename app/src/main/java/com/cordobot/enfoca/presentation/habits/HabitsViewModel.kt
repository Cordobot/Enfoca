package com.cordobot.enfoca.presentation.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cordobot.enfoca.domain.model.HabitProgress
import com.cordobot.enfoca.domain.repository.HabitRepository
import com.cordobot.enfoca.domain.usecase.ToggleHabitUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HabitsUiState(
    val habits: List<HabitProgress> = emptyList(),
    val isLoading: Boolean = true
) {
    val doneToday: Int get() = habits.count { it.doneToday }
    val todayProgress: Float get() = if (habits.isEmpty()) 0f else doneToday.toFloat() / habits.size
}

@HiltViewModel
class HabitsViewModel @Inject constructor(
    repository: HabitRepository,
    private val toggleHabit: ToggleHabitUseCase
) : ViewModel() {

    val uiState: StateFlow<HabitsUiState> = repository.observeProgress()
        .map { HabitsUiState(habits = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HabitsUiState())

    fun onToggle(id: Long) {
        viewModelScope.launch { toggleHabit(id) }
    }
}
