package com.focustree.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sync_outbox` (
                    `session_id` TEXT NOT NULL,
                    `queued_at_epoch_millis` INTEGER NOT NULL,
                    `attempt_count` INTEGER NOT NULL,
                    `last_error_code` TEXT,
                    PRIMARY KEY(`session_id`),
                    FOREIGN KEY(`session_id`) REFERENCES `focus_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_sync_outbox_queued_at_epoch_millis`
                ON `sync_outbox` (`queued_at_epoch_millis`)
                """.trimIndent(),
            )
        }
    }
