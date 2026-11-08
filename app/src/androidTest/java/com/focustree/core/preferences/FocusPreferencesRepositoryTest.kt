package com.focustree.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FocusPreferencesRepositoryTest {
    private lateinit var scope: CoroutineScope
    private lateinit var repository: FocusPreferencesRepository

    @Before
    fun createRepository() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val dataStore =
            PreferenceDataStoreFactory.create(scope = scope) {
                context.preferencesDataStoreFile("preferences-${UUID.randomUUID()}")
            }
        repository = DataStoreFocusPreferencesRepository(dataStore)
    }

    @After
    fun closeRepository() {
        scope.cancel()
    }

    @Test
    fun themeAndDefaultDurationPersist() =
        runBlocking {
            repository.setTheme(ThemePreference.DARK)
            repository.setDefaultDurationMinutes(45)
            repository.setSyncEnabled(true)

            assertEquals(FocusPreferences(ThemePreference.DARK, 45, true), repository.preferences.first())
        }

    @Test
    fun invalidDefaultDurationIsRejectedWithoutChangingStoredValue() =
        runBlocking {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking { repository.setDefaultDurationMinutes(4) }
            }

            assertEquals(25, repository.preferences.first().defaultDurationMinutes)
        }
}
