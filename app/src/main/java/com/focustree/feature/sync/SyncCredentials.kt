package com.focustree.feature.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SyncCredentials(
    val installationId: String,
    val bearerToken: String,
)

interface SyncCredentialStore {
    fun read(): SyncCredentials?

    fun write(credentials: SyncCredentials)

    fun clear()
}

class AndroidKeystoreCredentialStore(
    context: Context,
) : SyncCredentialStore {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun read(): SyncCredentials? {
        val encoded = preferences.getString(ENCRYPTED_TOKEN, null)
        val installationId = preferences.getString(INSTALLATION_ID, null)
        return if (encoded == null || installationId == null) {
            null
        } else {
            val packed = Base64.decode(encoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, packed.copyOfRange(0, IV_BYTES)))
            val token = cipher.doFinal(packed.copyOfRange(IV_BYTES, packed.size)).decodeToString()
            SyncCredentials(installationId, token)
        }
    }

    override fun write(credentials: SyncCredentials) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val packed = cipher.iv + cipher.doFinal(credentials.bearerToken.toByteArray())
        preferences.edit(commit = true) {
            putString(INSTALLATION_ID, credentials.installationId)
            putString(ENCRYPTED_TOKEN, Base64.encodeToString(packed, Base64.NO_WRAP))
        }
    }

    override fun clear() {
        preferences.edit(commit = true) { clear() }
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec
                .Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFERENCES = "sync-credential-v1"
        const val INSTALLATION_ID = "installation_id"
        const val ENCRYPTED_TOKEN = "encrypted_bearer_token"
        const val KEY_ALIAS = "focus-tree-sync-token-v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val IV_BYTES = 12
        const val KEY_SIZE_BITS = 256
    }
}
