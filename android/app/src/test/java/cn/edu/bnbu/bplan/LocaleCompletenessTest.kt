package cn.edu.bnbu.bplan

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 自动化多语言完整性测试用例
 * 保证 values-en 与 values-zh-rHK 100% 覆盖 values/strings.xml 中的全部 string 键名。
 */
class LocaleCompletenessTest {

    private fun getStringsFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app", relativePath),
            File("../app", relativePath),
            File("android/app", relativePath)
        )
        val file = candidates.firstOrNull { it.exists() }
        assertNotNull("Strings XML file not found at path: $relativePath", file)
        return file!!
    }

    private fun extractStringKeys(file: File): Set<String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        doc.documentElement.normalize()

        val stringElements = doc.getElementsByTagName("string")
        val keys = mutableSetOf<String>()
        for (i in 0 until stringElements.length) {
            val node = stringElements.item(i)
            if (node is Element) {
                val name = node.getAttribute("name")
                if (name.isNotBlank()) {
                    keys.add(name)
                }
            }
        }
        return keys
    }

    @Test
    fun testDefaultStringsExistAndNotEmpty() {
        val defaultFile = getStringsFile("src/main/res/values/strings.xml")
        val defaultKeys = extractStringKeys(defaultFile)
        assertTrue("Default strings.xml must not be empty", defaultKeys.isNotEmpty())
    }

    @Test
    fun testValuesEnCoverage100Percent() {
        val defaultFile = getStringsFile("src/main/res/values/strings.xml")
        val enFile = getStringsFile("src/main/res/values-en/strings.xml")

        val defaultKeys = extractStringKeys(defaultFile)
        val enKeys = extractStringKeys(enFile)

        val missingKeys = defaultKeys - enKeys
        assertTrue(
            "values-en/strings.xml does not have 100% coverage! Missing ${missingKeys.size} key(s):\n" +
                missingKeys.sorted().joinToString("\n") { " - $it" },
            missingKeys.isEmpty()
        )
    }

    @Test
    fun testValuesZhRhkCoverage100Percent() {
        val defaultFile = getStringsFile("src/main/res/values/strings.xml")
        val hkFile = getStringsFile("src/main/res/values-zh-rHK/strings.xml")

        val defaultKeys = extractStringKeys(defaultFile)
        val hkKeys = extractStringKeys(hkFile)

        val missingKeys = defaultKeys - hkKeys
        assertTrue(
            "values-zh-rHK/strings.xml does not have 100% coverage! Missing ${missingKeys.size} key(s):\n" +
                missingKeys.sorted().joinToString("\n") { " - $it" },
            missingKeys.isEmpty()
        )
    }
}
