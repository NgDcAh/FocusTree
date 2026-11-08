package com.focustree.feature.history

import com.focustree.feature.timer.FocusSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.TimeZone

data class HistoryUiState(
    val loading: Boolean = true,
    val summary: HistorySummary? = null,
    val error: Boolean = false,
)

class HistoryViewModel(
    repository: HistoryRepository,
    scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val timeZone: () -> TimeZone = TimeZone::getDefault,
) {
    private val mutableState = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            repository
                .observeHistory()
                .catch { mutableState.value = HistoryUiState(loading = false, error = true) }
                .collect { sessions: List<FocusSession> ->
                    mutableState.value =
                        HistoryUiState(
                            loading = false,
                            summary = HistoryProjection.summarize(sessions, now(), timeZone()),
                        )
                }
        }
    }
}
