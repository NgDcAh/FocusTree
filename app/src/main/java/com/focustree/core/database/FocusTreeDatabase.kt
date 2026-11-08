package com.focustree.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.focustree.feature.timer.SessionOutcome

@Database(
    entities = [FocusSessionEntity::class, SyncOutboxEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(SessionOutcomeConverters::class)
abstract class FocusTreeDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao

    abstract fun syncOutboxDao(): SyncOutboxDao
}

class SessionOutcomeConverters {
    @TypeConverter
    fun fromStoredValue(value: String): SessionOutcome = SessionOutcome.valueOf(value)

    @TypeConverter
    fun toStoredValue(outcome: SessionOutcome): String = outcome.name
}
