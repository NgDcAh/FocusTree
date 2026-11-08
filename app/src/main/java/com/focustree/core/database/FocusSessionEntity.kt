package com.focustree.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.focustree.feature.timer.FocusSession
import com.focustree.feature.timer.SessionOutcome

private const val ACTIVE_SLOT = 1

@Entity(
    tableName = "focus_sessions",
    indices = [Index(value = ["active_slot"], unique = true)],
)
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val intention: String,
    @ColumnInfo(name = "planned_duration_millis") val plannedDurationMillis: Long,
    @ColumnInfo(name = "started_at_epoch_millis") val startedAtEpochMillis: Long,
    @ColumnInfo(name = "expected_end_at_epoch_millis") val expectedEndAtEpochMillis: Long,
    @ColumnInfo(name = "finished_at_epoch_millis") val finishedAtEpochMillis: Long?,
    val outcome: SessionOutcome,
    @ColumnInfo(name = "reward_granted") val rewardGranted: Boolean,
    @ColumnInfo(name = "active_slot") val activeSlot: Int?,
)

fun FocusSession.toEntity(): FocusSessionEntity =
    FocusSessionEntity(
        id = id,
        intention = intention,
        plannedDurationMillis = plannedDurationMillis,
        startedAtEpochMillis = startedAtEpochMillis,
        expectedEndAtEpochMillis = expectedEndAtEpochMillis,
        finishedAtEpochMillis = finishedAtEpochMillis,
        outcome = outcome,
        rewardGranted = rewardGranted,
        activeSlot = if (outcome == SessionOutcome.ACTIVE) ACTIVE_SLOT else null,
    )

fun FocusSessionEntity.toDomain(): FocusSession =
    FocusSession(
        id = id,
        intention = intention,
        plannedDurationMillis = plannedDurationMillis,
        startedAtEpochMillis = startedAtEpochMillis,
        expectedEndAtEpochMillis = expectedEndAtEpochMillis,
        finishedAtEpochMillis = finishedAtEpochMillis,
        outcome = outcome,
        rewardGranted = rewardGranted,
    )
