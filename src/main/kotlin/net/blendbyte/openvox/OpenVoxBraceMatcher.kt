package net.blendbyte.openvox

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = PAIRS
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, next: IElementType?) = true
    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int) = openingBraceOffset

    private companion object {
        val PAIRS = arrayOf(
            BracePair(T.LBRACE, T.RBRACE, true),
            BracePair(T.SELBRACE, T.RBRACE, false),
            BracePair(T.LBRACK, T.RBRACK, false),
            BracePair(T.LISTSTART, T.RBRACK, false),
            BracePair(T.LPAREN, T.RPAREN, false),
            BracePair(T.WSLPAREN, T.RPAREN, false),
        )
    }
}
