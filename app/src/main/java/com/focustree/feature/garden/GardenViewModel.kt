package com.focustree.feature.garden

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class GardenUiState(
    val isLoading: Boolean = true,
    val growthItems: List<GardenGrowth> = emptyList(),
    val hasError: Boolean = false,
)

class GardenViewModel(
    repository: GardenRepository,
    scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(GardenUiState())
    val state: StateFlow<GardenUiState> = mutableState.asStateFlow()

    init {
        scope.launch {
            repository
                .observeTodayGarden()
                .catch { mutableState.value = GardenUiState(isLoading = false, hasError = true) }
                .collect { growthItems ->
                    mutableState.value = GardenUiState(isLoading = false, growthItems = growthItems)
                }
        }
    }
}
