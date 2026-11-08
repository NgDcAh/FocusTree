package com.focustree

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PackageLayoutTest {
    @Test
    fun `foundation contains every planned package boundary`() {
        val sourceRoot = File("src/main/java/com/focustree")
        val expectedPackages =
            listOf(
                "app",
                "core/database",
                "core/model",
                "core/time",
                "core/notifications",
                "core/ui",
                "feature/home",
                "feature/timer",
                "feature/garden",
                "feature/history",
                "feature/settings",
            )

        expectedPackages.forEach { relativePath ->
            assertTrue(
                "Missing package directory: $relativePath",
                File(sourceRoot, relativePath).isDirectory,
            )
        }
    }
}
