package com.focustree.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseSchemaTest {
    @get:Rule
    val migrationHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            FocusTreeDatabase::class.java,
        )

    @Test
    fun versionOneDatabaseMigratesToVersionTwoWithoutLosingSessions() {
        migrationHelper.createDatabase(DATABASE_NAME, 1).close()
        migrationHelper.runMigrationsAndValidate(DATABASE_NAME, 2, true, MIGRATION_1_2).close()
    }

    private companion object {
        const val DATABASE_NAME = "migration-test"
    }
}
