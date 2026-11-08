@file:Suppress("MagicNumber")

package com.focustree.feature.garden

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.focustree.core.ui.FocusTreeTokens

@Composable
fun GardenScreen(repository: GardenRepository) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(repository) { GardenViewModel(repository, scope) }
    val state by viewModel.state.collectAsState()
    GardenScreenContent(state)
}

@Composable
fun GardenScreenContent(state: GardenUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(FocusTreeTokens.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(FocusTreeTokens.spacingMedium),
    ) {
        Text("Today's garden", style = MaterialTheme.typography.headlineMedium)
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.hasError -> {
                Text("Today's garden couldn't be loaded. Try again later.")
            }

            state.growthItems.isEmpty() -> {
                EmptyGarden()
            }

            else -> {
                Text(
                    text = completedSessionsLabel(state.growthItems.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 144.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FocusTreeTokens.spacingSmall),
                    verticalArrangement = Arrangement.spacedBy(FocusTreeTokens.spacingSmall),
                ) {
                    itemsIndexed(state.growthItems, key = { _, growth -> growth.sessionId }) { index, growth ->
                        GardenGrowthCard(growth, index + 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGarden() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = FocusTreeTokens.spacingExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FocusTreeTokens.spacingSmall),
    ) {
        Text("No plants yet today", style = MaterialTheme.typography.titleLarge)
        Text("Complete a focus session to grow one.")
        GardenPlantGlyph(ordinal = 0, modifier = Modifier.size(96.dp))
    }
}

@Composable
private fun GardenGrowthCard(
    growth: GardenGrowth,
    ordinal: Int,
) {
    val intention = growth.intention.ifBlank { "Focus session" }
    val durationMinutes = (growth.plannedDurationMillis / MILLIS_PER_MINUTE).coerceAtLeast(1L)
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .testTag(GARDEN_GROWTH_ITEM_TAG),
        shape = FocusTreeTokens.cardShape,
        border =
            androidx.compose.foundation.BorderStroke(
                FocusTreeTokens.pixelBorder,
                MaterialTheme.colorScheme.outlineVariant,
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(FocusTreeTokens.spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(FocusTreeTokens.spacingBase),
        ) {
            GardenPlantGlyph(ordinal, Modifier.size(72.dp))
            Text("Plant $ordinal", style = MaterialTheme.typography.labelLarge)
            Text(intention, style = MaterialTheme.typography.titleSmall)
            Text("$durationMinutes min completed", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GardenPlantGlyph(
    ordinal: Int,
    modifier: Modifier = Modifier,
) {
    val leaf = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier.semantics { hideFromAccessibility() }) {
        val pixel = 8.dp.toPx()
        val centerX = size.width / 2f
        val ground = size.height - pixel * 2
        val stemHeight = pixel * (3 + ordinal.mod(3))
        drawRect(
            FocusTreeTokens.soil,
            Offset(centerX - pixel * 3, ground),
            Size(pixel * 6, pixel),
        )
        drawRect(
            leaf,
            Offset(centerX - pixel / 2, ground - stemHeight),
            Size(pixel, stemHeight),
        )
        drawRect(
            leaf,
            Offset(centerX - pixel * 2, ground - stemHeight + pixel),
            Size(pixel * 1.5f, pixel),
        )
        drawRect(
            leaf,
            Offset(centerX + pixel / 2, ground - stemHeight + pixel * 2),
            Size(pixel * 1.5f, pixel),
        )
        if (ordinal.mod(2) == 0) {
            drawRect(
                Color(0xFFF2C14E),
                Offset(centerX - pixel, ground - stemHeight - pixel * 2),
                Size(pixel * 2, pixel * 2),
            )
        }
        drawRect(
            FocusTreeTokens.outlineLight,
            Offset(centerX - pixel * 3, ground - pixel),
            Size(pixel * 6, pixel),
            style = Stroke(width = FocusTreeTokens.pixelBorder.toPx()),
        )
    }
}

const val GARDEN_GROWTH_ITEM_TAG = "garden-growth-item"
private const val MILLIS_PER_MINUTE = 60_000L

private fun completedSessionsLabel(count: Int): String {
    val noun = if (count == 1) "session" else "sessions"
    return "$count completed focus $noun today"
}
