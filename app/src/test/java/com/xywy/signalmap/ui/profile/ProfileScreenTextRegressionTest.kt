package com.xywy.signalmap.ui.profile

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class ProfileScreenTextRegressionTest {
    @Test
    fun `custom tile source text does not contain placeholder question marks`() {
        val source = resolveProfileScreenSource().readText(Charsets.UTF_8)

        listOf(
            "\"??3??????????????????\"",
            "Text(\"??\")",
            "\"??? ? \${preset.coordinateSystem.name} ? \${preset.tileSize}px\"",
            "\"?????\"",
            "\"???????\"",
            "\"??URL????? {z}/{x}/{y}\"",
            "\"512????\"",
            "\"GCJ02??\"",
            "\"??/???????????WGS84????\"",
        ).forEach { token ->
            assertFalse(source.contains(token), "ProfileScreen.kt 仍包含占位乱码：$token")
        }
    }

    private fun resolveProfileScreenSource(): File {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(userDir, "app/src/main/java/com/xywy/signalmap/ui/profile/ProfileScreen.kt"),
            File(userDir, "src/main/java/com/xywy/signalmap/ui/profile/ProfileScreen.kt"),
        )
        return candidates.firstOrNull(File::exists)
            ?: error("找不到 ProfileScreen.kt，user.dir=${userDir.absolutePath}")
    }
}
