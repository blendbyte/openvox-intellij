package net.blendbyte.openvox

import com.intellij.testFramework.UsefulTestCase
import net.blendbyte.openvox.highlighting.OpenVoxColors
import net.blendbyte.openvox.highlighting.OpenVoxSyntaxHighlighter
import net.blendbyte.openvox.lexer.EppLexerAdapter
import net.blendbyte.openvox.lexer.OpenVoxLexerAdapter
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class HighlighterAndLexerTest : UsefulTestCase() {

    fun testSkippedTokensAreNotPaintedAsComments() {
        val h = OpenVoxSyntaxHighlighter()
        assertEquals(OpenVoxColors.STRING, h.getTokenHighlights(T.HD_TEXT).single())
        assertEquals(OpenVoxColors.STRING, h.getTokenHighlights(T.HD_END).single())
        assertEquals(OpenVoxColors.EPP_TAG, h.getTokenHighlights(T.EPP_START).single())
        assertEquals(OpenVoxColors.EPP_TAG, h.getTokenHighlights(T.EPP_END_TRIM).single())
        assertEquals(OpenVoxColors.COMMENT, h.getTokenHighlights(T.LINE_COMMENT).single())
    }

    fun testLexerStateDoesNotLeakBetweenRuns() {
        val lexer = OpenVoxLexerAdapter()
        lexer.start("\$x = @(EOT)\nstill inside the body\n")
        while (lexer.tokenType != null) lexer.advance()

        lexer.start("\$y = 1\n\$z = 2\n")
        val types = generateSequence { lexer.tokenType?.also { lexer.advance() } }.toList()
        assertFalse("heredoc state leaked into the next file", types.contains(T.HD_TEXT))
        assertTrue(types.contains(T.VARIABLE))
        assertTrue(types.contains(T.NUMBER))
    }

    fun testEppLexerRestartsInsideATag() {
        val text = "hello <%= \$name %> bye"
        val full = EppLexerAdapter()
        full.start(text)
        var stateInsideTag = -1
        var offsetInsideTag = -1
        while (full.tokenType != null) {
            if (full.tokenType == T.VARIABLE) {
                stateInsideTag = full.state
                offsetInsideTag = full.tokenStart
                break
            }
            full.advance()
        }
        assertTrue("no variable found inside the tag", offsetInsideTag > 0)

        val restarted = EppLexerAdapter()
        restarted.start(text, offsetInsideTag, text.length, stateInsideTag)
        assertEquals("restart inside a tag must not fall back to template text",
            T.VARIABLE, restarted.tokenType)
    }

    fun testEppLexerStartsInTemplateText() {
        val lexer = EppLexerAdapter()
        lexer.start("plain text <% 1 %>")
        assertEquals(T.EPP_TEXT, lexer.tokenType)
    }
}
