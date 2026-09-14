package net.blendbyte.openvox

import com.intellij.testFramework.ParsingTestCase
import net.blendbyte.openvox.parser.OpenVoxParserDefinition

class SmokeParsingTest : ParsingTestCase("", "pp", OpenVoxParserDefinition()) {
    override fun getTestDataPath() = "src/test/testData"

    fun testCoreConstructsParse() {
        val samples = listOf(
            "num" to "1",
            "str" to "'a'",
            "var" to "\$x",
            "assign" to "\$x = 1",
            "add" to "\$x = 1 + 2",
            "array" to "\$x = [1, 2]",
            "hash" to "\$x = { 'a' => 1 }",
            "call" to "include('x')",
            "emptyclass" to "class foo { }",
            "resource" to "notify { 'x': }",
            "chain" to "File['a'] -> Service['b']",
            "dq" to "\$x = \"a\${'$'}{1}b\"",
        )
        val failures = mutableListOf<String>()
        for ((name, code) in samples) {
            try {
                val f = createPsiFile(name, code)
                ensureParsed(f)
                val txt = toParseTreeText(f, true, false)
                if (txt.contains("PsiErrorElement")) failures += "$name: ERROR ELEMENT in `$code`"
            } catch (t: Throwable) {
                failures += "$name: ${t.javaClass.simpleName} in `$code`"
            }
        }
        if (failures.isNotEmpty()) fail(failures.joinToString("\n"))
    }
}
