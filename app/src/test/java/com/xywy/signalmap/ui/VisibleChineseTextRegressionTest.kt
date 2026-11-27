package com.xywy.signalmap.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class VisibleChineseTextRegressionTest {
    @Test
    fun `main visible kotlin sources do not contain obvious mojibake text`() {
        val sourceRoot = resolveSourceRoot()
        val suspiciousTokens = listOf(
            "\"??",
            "??\"",
            "鍩虹珯",
            "鍗犵敤",
            "鎴戠殑",
            "鏍呮牸",
            "鐡︾墖",
            "鑷畾",
            "娴嬭瘯",
            "绔欑偣",
        )

        val hits = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "java") }
            .flatMap { file ->
                file.readLines(Charsets.UTF_8).mapIndexedNotNull { index, line ->
                    val token = suspiciousTokens.firstOrNull(line::contains) ?: return@mapIndexedNotNull null
                    "${file.relativeTo(sourceRoot)}:${index + 1}: $token"
                }
            }
            .toList()

        assertTrue(hits.isEmpty(), "发现疑似中文乱码：${hits.take(20).joinToString()}")
    }

    private fun resolveSourceRoot(): File {
        val userDir = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(userDir, "app/src/main/java"),
            File(userDir, "src/main/java"),
        )
        return candidates.firstOrNull(File::exists)
            ?: error("找不到 main/java，user.dir=${userDir.absolutePath}")
    }
}
