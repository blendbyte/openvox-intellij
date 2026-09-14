package net.blendbyte.openvox.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import net.blendbyte.openvox.psi.OpenVoxTypes

object OpenVoxParserUtil : GeneratedParserUtilBase() {

    private val STATEMENT_CALLS = setOf(
        "require", "realize", "include", "contain", "tag",
        "debug", "info", "notice", "warning", "err",
        "fail", "import", "break", "next", "return",
    )

    @JvmStatic
    fun statementFunctionName(builder: PsiBuilder, level: Int): Boolean {
        val type = builder.tokenType
        if (type !== OpenVoxTypes.IDENTIFIER && type !== OpenVoxTypes.BARE_WORD) return false
        if (builder.tokenText !in STATEMENT_CALLS) return false
        val next = builder.lookAhead(1)
        if (next === OpenVoxTypes.LPAREN || next === OpenVoxTypes.WSLPAREN) return false
        builder.advanceLexer()
        return true
    }
}
