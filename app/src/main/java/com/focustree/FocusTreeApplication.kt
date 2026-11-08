package com.focustree

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.focustree.BuildConfig
import com.focustree.core.database.FocusTreeDatabase
import com.focustree.core.database.MIGRATION_1_2
import com.focustree.core.database.RoomFocusSessionStore
import com.focustree.core.preferences.DataStoreFocusPreferencesRepository
import com.focustree.core.preferences.FocusPreferencesRepository
import com.focustree.feature.sync.AndroidKeystoreCredentialStore
import com.focustree.feature.sync.HttpsSyncApi
import com.focustree.feature.sync.SyncCredentialStore
import com.focustree.feature.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Application.preferenceStore by preferencesDataStore(name = "focus-preferences")

class FocusTreeApplication : Application() {
    val syncCredentials: SyncCredentialStore by lazy { AndroidKeystoreCredentialStore(this) }
    val syncScheduler: SyncScheduler by lazy { SyncScheduler(this) }
    val database: FocusTreeDatabase by lazy {
        Room
            .databaseBuilder(this, FocusTreeDatabase::class.java, "focus-tree.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    val sessions: RoomFocusSessionStore by lazy {
        RoomFocusSessionStore(database.focusSessionDao(), database.syncOutboxDao())
    }

    val preferences: FocusPreferencesRepository by lazy {
        DataStoreFocusPreferencesRepository(preferenceStore)
    }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            if (preferences.preferences.first().syncEnabled) syncScheduler.enable() else syncScheduler.disable()
        }
    }

    suspend fun setSyncEnabled(enabled: Boolean) {
        preferences.setSyncEnabled(enabled)
        if (enabled) {
            syncScheduler.enable()
        } else {
            syncScheduler.disable()
            withContext(Dispatchers.IO) {
                val credentials = syncCredentials.read()
                if (credentials != null && BuildConfig.SYNC_API_URL.isNotBlank()) {
                    runCatching { HttpsSyncApi(BuildConfig.SYNC_API_URL).revoke(credentials) }
                }
                syncCredentials.clear()
            }
        }
    }
}
