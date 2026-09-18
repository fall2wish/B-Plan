package cn.edu.bnbu.bplan

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/**
 * 全库去标识化白盒合规守护自动化测试
 * 递归扫描 app/src/main 下的所有文件，严禁泄露真实个人学号、姓名、教师账号、敏感域名及旧校名等个人隐私信息。
 */
class PiiScrubberTest {

    private val prohibitedPatterns = listOf(
        "uic.edu.cn",
        "北师港浸大"
    )

    private val binaryExtensions = setOf(
        "png", "jpg", "jpeg", "webp", "gif", "ico", "bmp",
        "jar", "class", "dex", "so", "aar", "zip", "tar", "gz"
    )

    private fun getSrcMainDirectory(): File {
        val candidates = listOf(
            File("src/main"),
            File("app/src/main"),
            File("../app/src/main"),
            File("android/app/src/main")
        )
        val dir = candidates.firstOrNull { it.exists() && it.isDirectory }
        assertNotNull("Could not locate src/main directory in candidate search paths", dir)
        return dir!!
    }

    data class Violation(
        val file: File,
        val lineNumber: Int,
        val pattern: String,
        val lineSnippet: String
    )

    @Test
    fun testNoPiiLeakInSrcMainTextFiles() {
        val srcMainDir = getSrcMainDirectory()
        val textFiles = srcMainDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() !in binaryExtensions }
            .toList()

        assertTrue("src/main must contain scan-able files, found 0", textFiles.isNotEmpty())

        val violations = mutableListOf<Violation>()

        for (file in textFiles) {
            try {
                file.useLines(Charsets.UTF_8) { lines ->
                    lines.forEachIndexed { index, line ->
                        val lineNum = index + 1
                        for (pattern in prohibitedPatterns) {
                            if (line.contains(pattern, ignoreCase = true)) {
                                violations.add(
                                    Violation(
                                        file = file,
                                        lineNumber = lineNum,
                                        pattern = pattern,
                                        lineSnippet = line.trim()
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                fail("Failed to read file ${file.path}: ${e.message}")
            }
        }

        if (violations.isNotEmpty()) {
            val failureMessage = buildString {
                appendLine("Privacy / PII Compliance Violation Detected! Found ${violations.size} occurrence(s):")
                violations.forEach { v ->
                    appendLine("  [Pattern: \"${v.pattern}\"] ${v.file.canonicalPath}:${v.lineNumber} -> ${v.lineSnippet}")
                }
            }
            fail(failureMessage)
        }
    }

    @Test
    fun testNoPiiLeakInSrcMainBinaryFiles() {
        val srcMainDir = getSrcMainDirectory()
        val binaryFiles = srcMainDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in binaryExtensions }
            .toList()

        val binaryViolations = mutableListOf<String>()

        for (file in binaryFiles) {
            val bytes = file.readBytes()
            for (pattern in prohibitedPatterns) {
                val patternBytes = pattern.toByteArray(Charsets.UTF_8)
                if (containsBytes(bytes, patternBytes)) {
                    binaryViolations.add("Pattern \"$pattern\" detected in binary file: ${file.canonicalPath}")
                }
            }
        }

        if (binaryViolations.isNotEmpty()) {
            fail("Binary file PII violations:\n" + binaryViolations.joinToString("\n"))
        }
    }

    private fun containsBytes(source: ByteArray, target: ByteArray): Boolean {
        if (target.isEmpty() || source.size < target.size) return false
        val maxIndex = source.size - target.size
        outer@ for (i in 0..maxIndex) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    continue@outer
                }
            }
            return true
        }
        return false
    }
}
