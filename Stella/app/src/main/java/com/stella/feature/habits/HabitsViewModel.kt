package com.stella.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stella.core.database.dao.HabitDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habitDao: HabitDao,
) : ViewModel() {

    private val _state = MutableStateFlow(HabitsUiState())
    val state: StateFlow<HabitsUiState> = _state.asStateFlow()

    init {
        onEvent(HabitsUiEvent.Refresh)
    }

    fun onEvent(event: HabitsUiEvent) {
        when (event) {
            HabitsUiEvent.Refresh -> observeHabits()
        }
    }

    private fun observeHabits() {
        viewModelScope.launch {
            habitDao.observeActiveHabits().collect { habits ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        habits = habits.map { entity -> HabitRowUi(entity.id, entity.name) },
                        error = null,
                    )
                }
            }
        }
    }
}
