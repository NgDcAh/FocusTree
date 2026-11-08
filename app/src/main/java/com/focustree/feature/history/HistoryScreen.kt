package com.focustree.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.focustree.core.database.RoomFocusSessionStore
import com.focustree.feature.timer.SessionOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private const val MILLIS_PER_MINUTE = 60_000L
private const val CONTENT_PADDING_DP = 24
private const val CARD_PADDING_DP = 16
private const val ITEM_SPACING_DP = 8

@Composable
fun HistoryScreen(sessions: RoomFocusSessionStore) {
    val repository = remember(sessions) { RoomHistoryRepository(sessions) }
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    val viewModel = remember(repository) { HistoryViewModel(repository, scope) }
    HistoryContent(viewModel)
}

@Composable
fun HistoryContent(viewModel: HistoryViewModel) {
    val state by viewModel.state.collectAsState()
    Column(
        Modifier.fillMaxSize().padding(CONTENT_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(CONTENT_PADDING_DP.dp),
    ) {
        Text("Your focus", style = MaterialTheme.typography.headlineMedium)
        if (state.loading) {
            CircularProgressIndicator(Modifier.testTag("history-loading"))
        } else if (state.error) {
            Text("Focus history is unavailable. Try again later.")
        } else {
            val summary = state.summary
            if (summary == null || summary.sessions.isEmpty()) {
                Text("No sessions yet. Start a focus session to see your progress here.")
            } else {
                Text("Today · ${summary.todayCompletedMillis / MILLIS_PER_MINUTE} min completed")
                Text("Last 7 days · ${summary.sevenDayCompletedMillis.sum() / MILLIS_PER_MINUTE} min")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    summary.sevenDayCompletedMillis.forEachIndexed { index, millis ->
                        Column {
                            Text("${millis / MILLIS_PER_MINUTE}", style = MaterialTheme.typography.labelSmall)
                            Text(summary.sevenDayLabels[index], style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ITEM_SPACING_DP.dp),
                ) {
                    items(summary.sessions, key = { it.id }) { session ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(CARD_PADDING_DP.dp)) {
                                Text(
                                    session.intention.ifBlank { "Focus session" },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    if (session.outcome ==
                                        SessionOutcome.COMPLETED
                                    ) {
                                        "Completed · ${session.plannedDurationMillis / MILLIS_PER_MINUTE} min"
                                    } else {
                                        "Abandoned · no growth"
                                    },
                                )
                                Text(
                                    java.text.DateFormat.getDateTimeInstance().format(
                                        java.util.Date(session.finishedAtEpochMillis ?: session.startedAtEpochMillis),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
