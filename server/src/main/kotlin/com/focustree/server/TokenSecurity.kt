package com.focustree.server

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object TokenSecurity {
    private val secureRandom = SecureRandom()

    fun newToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun sha256(token: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))

    private const val TOKEN_BYTES = 32
}
