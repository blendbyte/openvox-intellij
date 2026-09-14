package net.blendbyte.openvox.lexer

import com.intellij.lexer.LexerBase
import com.intellij.lexer.RestartableLexer
import com.intellij.lexer.TokenIterator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.tree.IElementType

open class OpenVoxLexerAdapter(private val epp: Boolean = false) : LexerBase(), RestartableLexer {
    private val lexer = _OpenVoxLexer(null)
    private var buffer: CharSequence = ""
    private var end = 0
    private var token: IElementType? = null
    private var tokenState = 0

    override fun getStartState(): Int =
        (if (epp) _OpenVoxLexer.EPP_TEXT_ST else _OpenVoxLexer.YYINITIAL) or _OpenVoxLexer.REGEX_STATE
    override fun isRestartableState(state: Int): Boolean = state and _OpenVoxLexer.COMPLEX_STATE == 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        end = endOffset
        val replay = !isRestartableState(initialState)
        val state = if (startOffset == 0 || replay) startState else initialState
        lexer.reset(buffer, if (replay) 0 else startOffset, endOffset, state and 15)
        lexer.restoreContext(state)
        advance()
        if (replay) {
            while (token != null && tokenStart < startOffset) advance()
        }
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int, tokenIterator: TokenIterator?) =
        start(buffer, startOffset, endOffset, initialState)

    override fun advance() {
        ProgressManager.checkCanceled()
        tokenState = lexer.contextState()
        token = lexer.advance()
    }

    override fun getState(): Int = tokenState
    override fun getTokenType(): IElementType? = token
    override fun getTokenStart(): Int = lexer.tokenStart
    override fun getTokenEnd(): Int = lexer.tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = end
}

class EppLexerAdapter : OpenVoxLexerAdapter(epp = true)
