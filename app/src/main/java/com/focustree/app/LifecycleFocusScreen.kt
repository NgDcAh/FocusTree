package com.focustree.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.focustree.core.database.RoomFocusSessionStore
import com.focustree.core.notifications.CompletionNotifications
import com.focustree.core.notifications.SessionCompletionScheduler
import com.focustree.core.time.SystemClock
import com.focustree.core.ui.PixelGardenMotif
import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.MAX_INTENTION_LENGTH
import com.focustree.feature.timer.PRESET_DURATIONS_MINUTES
import com.focustree.feature.timer.SessionSetup
import com.focustree.feature.timer.SessionSetupValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

private const val MINUTE_MILLIS = 60_000L
private const val SECOND_MILLIS = 1_000L
private const val DEFAULT_DURATION_MINUTES = 25
private const val SECONDS_PER_MINUTE = 60L

@Composable
fun LifecycleFocusScreen(
    sessions: RoomFocusSessionStore,
    defaultDurationMinutes: Int = DEFAULT_DURATION_MINUTES,
) {
    val context = LocalContext.current
    val scheduler = remember { SessionCompletionScheduler(context) }
    val scope = rememberCoroutineScope()
    var session by remember { mutableStateOf<FocusSession?>(null) }
    var intention by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(DEFAULT_DURATION_MINUTES) }
    var remaining by remember { mutableLongStateOf(0L) }
    var message by remember { mutableStateOf("") }
    LaunchedEffect(defaultDurationMinutes, session) {
        if (session == null) duration = defaultDurationMinutes
    }
    val permissionRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) message = "Notifications are off. Your session will still be saved."
        }

    LaunchedEffect(sessions) {
        refreshTimer(
            sessions,
            scheduler,
            { session = it },
            { remaining = it },
            { message = it },
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SessionPanel(
                sessions,
                scheduler,
                session,
                intention,
                duration,
                remaining,
                message,
                { session = it },
                { intention = it },
                { duration = it },
                { remaining = it },
                { message = it },
                { permissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS) },
                scope,
                context,
            )
        }
    }
}

@Composable
private fun SessionPanel(
    sessions: RoomFocusSessionStore,
    scheduler: SessionCompletionScheduler,
    session: FocusSession?,
    intention: String,
    duration: Int,
    remaining: Long,
    message: String,
    setSession: (FocusSession?) -> Unit,
    setIntention: (String) -> Unit,
    setDuration: (Int) -> Unit,
    setRemaining: (Long) -> Unit,
    setMessage: (String) -> Unit,
    requestNotificationPermission: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
) {
    Text("Focus Tree", style = MaterialTheme.typography.headlineMedium)
    PixelGardenMotif()
    if (session == null) {
        SetupContent(intention, duration, message, setIntention, setDuration) {
            setMessage("")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !CompletionNotifications.hasPermission(context)
            ) {
                requestNotificationPermission()
            }
            scope.launch {
                val result = startSession(sessions, scheduler, intention, duration)
                setSession(result.first)
                setRemaining(result.first?.plannedDurationMillis ?: 0L)
                setMessage(result.second)
            }
        }
    } else {
        val active = session
        ActiveContent(active, remaining) {
            scope.launch {
                sessions.abandon(active.id, SystemClock.nowEpochMillis())
                scheduler.cancel(active.id)
                setSession(null)
                setRemaining(0L)
                setMessage("Session ended. No reward was added.")
            }
        }
    }
    if (message.isNotBlank()) Text(message)
}

@Composable
private fun SetupContent(
    intention: String,
    duration: Int,
    message: String,
    onIntention: (String) -> Unit,
    onDuration: (Int) -> Unit,
    onStart: () -> Unit,
) {
    Text("Start a focus session", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(
        value = intention,
        onValueChange = { onIntention(it.take(MAX_INTENTION_LENGTH)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Intention (optional)") },
        supportingText = { Text("${intention.length}/$MAX_INTENTION_LENGTH") },
    )
    Text("Duration", style = MaterialTheme.typography.titleMedium)
    PRESET_DURATIONS_MINUTES.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { minutes ->
                FilterChip(
                    selected = duration == minutes,
                    onClick = { onDuration(minutes) },
                    colors =
                        FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    label = { Text("$minutes min") },
                )
            }
        }
    }
    Button(onClick = onStart) { Text("Start focus") }
    if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun ActiveContent(
    session: FocusSession,
    remaining: Long,
    onEnd: () -> Unit,
) {
    Text(session.intention.ifBlank { "Stay with your focus" }, style = MaterialTheme.typography.titleLarge)
    Text(formatDuration(remaining), style = MaterialTheme.typography.displayLarge)
    Text("Your timer restores from its saved end time when you return.")
    TextButton(onClick = onEnd) { Text("End session") }
}

private suspend fun refreshTimer(
    sessions: RoomFocusSessionStore,
    scheduler: SessionCompletionScheduler,
    setSession: (FocusSession?) -> Unit,
    setRemaining: (Long) -> Unit,
    setMessage: (String) -> Unit,
) {
    while (true) {
        val active = sessions.activeSession()
        val now = SystemClock.nowEpochMillis()
        if (active != null && now >= active.expectedEndAtEpochMillis) {
            sessions.complete(active.id, now)
            scheduler.cancel(active.id)
            setSession(null)
            setRemaining(0L)
            setMessage("Focus session complete.")
        } else {
            setSession(active)
            setRemaining(active?.let { (it.expectedEndAtEpochMillis - now).coerceAtLeast(0L) } ?: 0L)
        }
        delay(SECOND_MILLIS)
    }
}

private suspend fun startSession(
    sessions: RoomFocusSessionStore,
    scheduler: SessionCompletionScheduler,
    intention: String,
    duration: Int,
): Pair<FocusSession?, String> {
    val setup = SessionSetup(intention.trim(), duration)
    if (SessionSetupValidator.validate(setup.intention, setup.durationMinutes).isNotEmpty()) {
        return null to "Choose a duration from 5 to 120 minutes."
    }
    val now = SystemClock.nowEpochMillis()
    val durationMillis = duration * MINUTE_MILLIS
    val created =
        FocusSession(
            UUID.randomUUID().toString(),
            setup.intention,
            durationMillis,
            now,
            now + durationMillis,
        )
    return runCatching {
        sessions.insert(created)
        scheduler.schedule(created.id, created.expectedEndAtEpochMillis, now)
        created to ""
    }.getOrElse { null to "A focus session is already active." }
}

private fun formatDuration(millis: Long): String {
    val seconds = millis / SECOND_MILLIS
    return "%02d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}
