package com.xywy.signalmap.domain.model

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertTrue

class FileProviderPathsTest {
    @Test
    fun `file provider exposes both log and layer export cache folders`() {
        val text = readUtf8(projectRoot().resolve("app/src/main/res/xml/file_paths.xml"))

        assertTrue(text.contains("shared-logs/"), "测试记录分享目录必须在 FileProvider 中注册")
        assertTrue(text.contains("shared-layers/"), "图层 CSV/KML 导出目录必须在 FileProvider 中注册")
    }

    private fun projectRoot(): Path {
        val userDir = Path.of(System.getProperty("user.dir"))
        val candidates = listOf(userDir, userDir.parent).filterNotNull()
        return candidates.firstOrNull { Files.exists(it.resolve("app/src/main/AndroidManifest.xml")) }
            ?: userDir
    }

    private fun readUtf8(path: Path): String = Files.readAllBytes(path).toString(Charsets.UTF_8)
}
