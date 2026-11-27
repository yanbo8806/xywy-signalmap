package com.xywy.signalmap.domain.model

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class NetworkSecurityConfigTest {
    @Test
    fun `manifest allows cleartext for local tile proxy`() {
        val root = projectRoot()
        val manifest = readUtf8(root.resolve("app/src/main/AndroidManifest.xml"))
        val networkConfig = root.resolve("app/src/main/res/xml/network_security_config.xml")

        assertTrue(manifest.contains("networkSecurityConfig"), "Manifest should reference network security config")
        assertTrue(Files.exists(networkConfig), "network_security_config.xml should exist")

        val configText = readUtf8(networkConfig)
        assertTrue(!configText.contains("<domain "), "Config should not include private domain exceptions")
        assertTrue(configText.contains("cleartextTrafficPermitted=\"true\""), "Config should permit cleartext traffic")
        assertTrue(configText.contains("<base-config"), "Config should allow localhost cleartext proxy traffic")
    }

    private fun projectRoot(): Path {
        val userDir = Path.of(System.getProperty("user.dir"))
        val candidates = listOf(userDir, userDir.parent).filterNotNull()
        return candidates.firstOrNull { Files.exists(it.resolve("app/src/main/AndroidManifest.xml")) }
            ?: userDir
    }

    private fun readUtf8(path: Path): String = Files.readAllBytes(path).toString(Charsets.UTF_8)
}
