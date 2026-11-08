package com.focustree.feature.garden

import com.focustree.core.database.RoomFocusSessionStore
import com.focustree.core.time.Clock
import com.focustree.core.time.SystemClock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.TimeZone

interface GardenRepository {
    fun observeTodayGarden(): Flow<List<GardenGrowth>>
}

class RoomGardenRepository(
    private val sessions: RoomFocusSessionStore,
    private val clock: Clock = SystemClock,
    private val timeZone: () -> TimeZone = TimeZone::getDefault,
) : GardenRepository {
    override fun observeTodayGarden(): Flow<List<GardenGrowth>> =
        sessions.observeSessionHistory().map { records ->
            GardenProjection.projectToday(
                records,
                GardenProjection.todayWindow(clock.nowEpochMillis(), timeZone()),
            )
        }
}
