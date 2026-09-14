package net.blendbyte.openvox

import com.intellij.lexer.Lexer
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import net.blendbyte.openvox.highlighting.OpenVoxSyntaxHighlighter
import net.blendbyte.openvox.lexer.OpenVoxLexerAdapter

class OpenVoxIncrementalLexerTest : BasePlatformTestCase() {
    private val manifest = """
        ${'$'}x = "outer ${'$'}{["nested ${'$'}{facts['os']}", {'key' => 1}]} tail"
        ${'$'}a = @(FIRST)
        first body
        FIRST
        ${'$'}pair = [@(SECOND), @(THIRD)]
        second body
        SECOND
        third body
        THIRD
        ${'$'}ratio = 12 / 3 / 2
        ${'$'}match = 'x' =~ /x/
        ${'$'}choice = true ? { true => 1, default => 0 }
    """.trimIndent()

    private fun tokens(lexer: Lexer): List<String> = buildList {
        while (lexer.tokenType != null) {
            add("${lexer.tokenType}:${lexer.tokenText}")
            lexer.advance()
        }
    }

    private fun checkEveryRestart(text: String, epp: Boolean) {
        val full = OpenVoxLexerAdapter(epp)
        full.start(text)
        val starts = mutableListOf<Pair<Int, Int>>()
        val expected = buildList {
            while (full.tokenType != null) {
                starts += full.tokenStart to full.state
                add("${full.tokenType}:${full.tokenText}")
                full.advance()
            }
        }
        starts.forEachIndexed { index, (offset, state) ->
            val lexer = OpenVoxLexerAdapter(epp)
            lexer.start(text, offset, text.length, state)
            assertEquals("Restart at $offset, state $state", expected.drop(index), tokens(lexer))
            assertTrue("State must fit highlighter storage", state in 0..32767)
        }
    }

    fun testRestartAtEveryManifestToken() = checkEveryRestart(manifest, false)
    fun testRestartAtEveryEppToken() = checkEveryRestart("before <% $manifest %> after <%= \$x %>", true)

    private fun assertEditorMatchesFullLexing(epp: Boolean = false) {
        val text = myFixture.editor.document.text
        val lexer = OpenVoxLexerAdapter(epp)
        lexer.start(text)
        val iterator = (myFixture.editor as EditorEx).highlighter.createIterator(0)
        val actual = buildList {
            while (!iterator.atEnd()) {
                add("${iterator.tokenType}:${text.substring(iterator.start, iterator.end)}")
                iterator.advance()
            }
        }
        assertEquals(tokens(lexer), actual)
    }

    private fun edit(old: String, replacement: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            val document = myFixture.editor.document
            val start = document.text.indexOf(old)
            assertTrue("Missing edit target: $old", start >= 0)
            document.replaceString(start, start + old.length, replacement)
        }
    }

    fun testEditorUpdatesInterpolationAndHeredocTerminators() {
        myFixture.configureByText("site.pp", manifest)
        assertEditorMatchesFullLexing()
        edit("facts['os']", "facts['networking']")
        assertEditorMatchesFullLexing()
        edit("@(FIRST)", "@(OTHER)")
        assertEditorMatchesFullLexing()
        edit("\nFIRST\n", "\nOTHER\n")
        assertEditorMatchesFullLexing()
        edit("${'$'}{[", "${'$'}{{'nested' => [")
        assertEditorMatchesFullLexing()
    }

    fun testEditorUpdatesEppTags() {
        myFixture.configureByText("template.epp", "before <%= \"hello \${facts['os']}\" %> after")
        edit("facts['os']", "facts['networking']")
        assertEditorMatchesFullLexing(true)
        edit("%>", "-%>")
        assertEditorMatchesFullLexing(true)
    }

    fun testOrdinaryEditDoesNotRelexWholeFile() {
        myFixture.configureByText("site.pp", (1..2000).joinToString("\n") { "\$value$it = $it" })
        val counting = object : OpenVoxLexerAdapter() {
            var advances = 0
            override fun advance() { advances++; super.advance() }
        }
        val editor = myFixture.editor as EditorEx
        editor.highlighter = LexerEditorHighlighter(object : OpenVoxSyntaxHighlighter() {
            override fun getHighlightingLexer(): Lexer = counting
        }, editor.colorsScheme)
        counting.advances = 0
        edit("= 1999", "= 9999")
        assertEditorMatchesFullLexing()
        assertTrue("Relexed ${counting.advances} tokens", counting.advances < 100)
    }
}
