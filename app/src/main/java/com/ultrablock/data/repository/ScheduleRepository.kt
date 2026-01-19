package com.ultrablock.data.repository

import com.ultrablock.data.local.dao.ScheduleDao
import com.ultrablock.data.local.entity.Schedule
import com.ultrablock.domain.model.BlockSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao
) {
    fun getAllSchedules(): Flow<List<BlockSchedule>> = scheduleDao.getAllSchedules().map { schedules ->
        schedules.map { it.toBlockSchedule() }
    }

    fun getEnabledSchedules(): Flow<List<BlockSchedule>> = scheduleDao.getEnabledSchedules().map { schedules ->
        schedules.map { it.toBlockSchedule() }
    }

    suspend fun addSchedule(schedule: BlockSchedule): Long {
        return scheduleDao.insert(schedule.toEntity())
    }

    suspend fun updateSchedule(schedule: BlockSchedule) {
        scheduleDao.update(schedule.toEntity())
    }

    suspend fun deleteSchedule(id: Long) {
        scheduleDao.deleteById(id)
    }

    suspend fun isWithinBlockingSchedule(): Boolean {
        val schedules = scheduleDao.getEnabledSchedulesList()
        if (schedules.isEmpty()) return false

        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        return schedules.any { schedule ->
            val isDayActive = when (currentDayOfWeek) {
                Calendar.MONDAY -> schedule.monday
                Calendar.TUESDAY -> schedule.tuesday
                Calendar.WEDNESDAY -> schedule.wednesday
                Calendar.THURSDAY -> schedule.thursday
                Calendar.FRIDAY -> schedule.friday
                Calendar.SATURDAY -> schedule.saturday
                Calendar.SUNDAY -> schedule.sunday
                else -> false
            }

            if (!isDayActive) return@any false

            // Handle schedules that span midnight
            if (schedule.startTimeMinutes <= schedule.endTimeMinutes) {
                currentMinutes in schedule.startTimeMinutes until schedule.endTimeMinutes
            } else {
                currentMinutes >= schedule.startTimeMinutes || currentMinutes < schedule.endTimeMinutes
            }
        }
    }

    private fun Schedule.toBlockSchedule(): BlockSchedule {
        val activeDays = mutableSetOf<DayOfWeek>()
        if (monday) activeDays.add(DayOfWeek.MONDAY)
        if (tuesday) activeDays.add(DayOfWeek.TUESDAY)
        if (wednesday) activeDays.add(DayOfWeek.WEDNESDAY)
        if (thursday) activeDays.add(DayOfWeek.THURSDAY)
        if (friday) activeDays.add(DayOfWeek.FRIDAY)
        if (saturday) activeDays.add(DayOfWeek.SATURDAY)
        if (sunday) activeDays.add(DayOfWeek.SUNDAY)

        return BlockSchedule(
            id = id,
            startTime = LocalTime.of(startTimeMinutes / 60, startTimeMinutes % 60),
            endTime = LocalTime.of(endTimeMinutes / 60, endTimeMinutes % 60),
            activeDays = activeDays,
            isEnabled = isEnabled
        )
    }

    private fun BlockSchedule.toEntity(): Schedule {
        return Schedule(
            id = id,
            startTimeMinutes = startTime.hour * 60 + startTime.minute,
            endTimeMinutes = endTime.hour * 60 + endTime.minute,
            monday = activeDays.contains(DayOfWeek.MONDAY),
            tuesday = activeDays.contains(DayOfWeek.TUESDAY),
            wednesday = activeDays.contains(DayOfWeek.WEDNESDAY),
            thursday = activeDays.contains(DayOfWeek.THURSDAY),
            friday = activeDays.contains(DayOfWeek.FRIDAY),
            saturday = activeDays.contains(DayOfWeek.SATURDAY),
            sunday = activeDays.contains(DayOfWeek.SUNDAY),
            isEnabled = isEnabled
        )
    }
}
