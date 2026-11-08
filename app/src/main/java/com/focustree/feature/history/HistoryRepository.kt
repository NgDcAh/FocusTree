package com.focustree.feature.history

import com.focustree.core.database.RoomFocusSessionStore
import com.focustree.feature.timer.FocusSession
import kotlinx.coroutines.flow.Flow

fun interface HistoryRepository {
    fun observeHistory(): Flow<List<FocusSession>>
}

class RoomHistoryRepository(
    private val sessions: RoomFocusSessionStore,
) : HistoryRepository {
    override fun observeHistory(): Flow<List<FocusSession>> = sessions.observeSessionHistory()
}
