package com.ultrablock.ui.screens.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ultrablock.data.repository.ScheduleRepository
import com.ultrablock.domain.model.BlockSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ScheduleUiState(
    val schedules: List<BlockSchedule> = emptyList(),
    val showAddDialog: Boolean = false,
    val editingSchedule: BlockSchedule? = null
)

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _showAddDialog = MutableStateFlow(false)
    private val _editingSchedule = MutableStateFlow<BlockSchedule?>(null)

    val uiState: StateFlow<ScheduleUiState> = combine(
        scheduleRepository.getAllSchedules(),
        _showAddDialog,
        _editingSchedule
    ) { schedules, showDialog, editing ->
        ScheduleUiState(
            schedules = schedules,
            showAddDialog = showDialog,
            editingSchedule = editing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScheduleUiState()
    )

    fun showAddDialog() {
        _editingSchedule.value = null
        _showAddDialog.value = true
    }

    fun showEditDialog(schedule: BlockSchedule) {
        _editingSchedule.value = schedule
        _showAddDialog.value = true
    }

    fun hideDialog() {
        _showAddDialog.value = false
        _editingSchedule.value = null
    }

    fun saveSchedule(
        startTime: LocalTime,
        endTime: LocalTime,
        activeDays: Set<DayOfWeek>,
        isEnabled: Boolean
    ) {
        viewModelScope.launch {
            val editing = _editingSchedule.value
            if (editing != null) {
                scheduleRepository.updateSchedule(
                    editing.copy(
                        startTime = startTime,
                        endTime = endTime,
                        activeDays = activeDays,
                        isEnabled = isEnabled
                    )
                )
            } else {
                scheduleRepository.addSchedule(
                    BlockSchedule(
                        startTime = startTime,
                        endTime = endTime,
                        activeDays = activeDays,
                        isEnabled = isEnabled
                    )
                )
            }
            hideDialog()
        }
    }

    fun deleteSchedule(schedule: BlockSchedule) {
        viewModelScope.launch {
            scheduleRepository.deleteSchedule(schedule.id)
        }
    }

    fun toggleScheduleEnabled(schedule: BlockSchedule) {
        viewModelScope.launch {
            scheduleRepository.updateSchedule(schedule.copy(isEnabled = !schedule.isEnabled))
        }
    }

    fun formatTime(time: LocalTime): String {
        return time.format(DateTimeFormatter.ofPattern("h:mm a"))
    }

    fun formatDays(days: Set<DayOfWeek>): String {
        if (days.size == 7) return "Every day"
        if (days.size == 5 && !days.contains(DayOfWeek.SATURDAY) && !days.contains(DayOfWeek.SUNDAY)) {
            return "Weekdays"
        }
        if (days.size == 2 && days.contains(DayOfWeek.SATURDAY) && days.contains(DayOfWeek.SUNDAY)) {
            return "Weekends"
        }

        return days.sortedBy { it.value }.joinToString(", ") { day ->
            when (day) {
                DayOfWeek.MONDAY -> "Mon"
                DayOfWeek.TUESDAY -> "Tue"
                DayOfWeek.WEDNESDAY -> "Wed"
                DayOfWeek.THURSDAY -> "Thu"
                DayOfWeek.FRIDAY -> "Fri"
                DayOfWeek.SATURDAY -> "Sat"
                DayOfWeek.SUNDAY -> "Sun"
            }
        }
    }
}
