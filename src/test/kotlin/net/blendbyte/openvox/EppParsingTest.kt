package net.blendbyte.openvox

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import net.blendbyte.openvox.parser.EppParserDefinition
import net.blendbyte.openvox.parser.OpenVoxParserDefinition

class EppParsingTest : ParsingTestCase("", "epp", EppParserDefinition(), OpenVoxParserDefinition()) {

    override fun getTestDataPath() = "src/test/testData"
    override fun skipSpaces() = false
    override fun includeRanges() = true

    private fun assertParses(name: String, code: String) {
        val file = createPsiFile(name, code)
        ensureParsed(file)
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        if (errors.isNotEmpty()) {
            val details = errors.joinToString("\n") { e ->
                val line = code.substring(0, e.textOffset).count { it == '\n' } + 1
                "  line $line: ${e.errorDescription} at '${e.text.take(40)}'"
            }
            fail("$name did not parse cleanly:\n$details\n--- tree ---\n${toParseTreeText(file, true, false)}")
        }
    }

    fun testEppTemplate() {
        val code = """
            <%- | Hash ${'$'}settings, Optional[String] ${'$'}note = undef | -%>
            # Managed by OpenVox
            <% ${'$'}settings.each |${'$'}key, ${'$'}value| { -%>
            <%= ${'$'}key %> = <%= ${'$'}value %>
            <% } -%>
            <%# a comment %>
        """.trimIndent()
        assertParses("template.epp", code)
    }

    fun testTrimmedCaseOptionsAcrossTags() {
        assertParses("case.epp", """
            <%- | String ${'$'}protocol | -%>
            <%- case ${'$'}protocol { -%>
              <%- 'imap': { -%>
            imap enabled
              <%- } -%>
              <%- default: { -%>
            fallback
              <%- } -%>
            <%- } -%>
        """.trimIndent())
    }
}
