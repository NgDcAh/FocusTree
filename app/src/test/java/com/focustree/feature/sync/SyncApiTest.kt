package com.focustree.feature.sync

import org.junit.Assert.assertThrows
import org.junit.Test

class SyncApiTest {
    @Test
    fun apiRejectsNonHttpsEndpoints() {
        assertThrows(IllegalArgumentException::class.java) {
            HttpsSyncApi("http://sync.example.test")
        }
    }
}
