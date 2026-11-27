package com.xywy.signalmap.ui.cell

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class CellScreenTextRegressionTest {
    @Test
    fun `neighbor card source does not contain placeholder question text`() {
        val source = resolveCellScreenSource().readText(Charsets.UTF_8)

        listOf(
            "joinToString(\" ? \")",
            "appendLine(\"?????",
            "appendLine(\"???\${details.channelValue ?: \\\"-\\\"}\")",
            "title = \"????\"",
            "secondary = \"??????????\"",
            "\"??\" to details.channelValue",
            "title = { Text(\"????\") }",
            "Text(\"??\")",
        ).forEach { token ->
            assertFalse(source.contains(token), "CellScreen.kt 仍包含占位乱码：$token")
        }
    }

    private fun resolveCellScreenSource(): File {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(userDir, "app/src/main/java/com/xywy/signalmap/ui/cell/CellScreen.kt"),
            File(userDir, "src/main/java/com/xywy/signalmap/ui/cell/CellScreen.kt"),
        )
        return candidates.firstOrNull(File::exists)
            ?: error("找不到 CellScreen.kt，user.dir=${userDir.absolutePath}")
    }
}
