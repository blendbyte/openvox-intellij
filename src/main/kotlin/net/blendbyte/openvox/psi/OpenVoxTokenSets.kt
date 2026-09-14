package net.blendbyte.openvox.psi

import com.intellij.psi.tree.TokenSet

object OpenVoxTokenSets {
    @JvmField
    val COMMENTS: TokenSet = TokenSet.create(
        OpenVoxTypes.LINE_COMMENT, OpenVoxTypes.BLOCK_COMMENT, OpenVoxTypes.EPP_COMMENT,
    )

    @JvmField
    val PARSER_SKIPPED: TokenSet = TokenSet.orSet(
        COMMENTS,
        TokenSet.create(
            OpenVoxTypes.HD_TEXT, OpenVoxTypes.HD_END,
            OpenVoxTypes.EPP_START, OpenVoxTypes.EPP_EXPR_START, OpenVoxTypes.EPP_START_TRIM,
            OpenVoxTypes.EPP_END, OpenVoxTypes.EPP_END_TRIM,
        ),
    )

    @JvmField
    val STRINGS: TokenSet = TokenSet.create(
        OpenVoxTypes.SQ_STRING, OpenVoxTypes.DQ_TEXT, OpenVoxTypes.DQ_START,
        OpenVoxTypes.DQ_END, OpenVoxTypes.HD_TEXT,
    )

    @JvmField
    val KEYWORDS: TokenSet = TokenSet.create(
        OpenVoxTypes.CASE, OpenVoxTypes.CLASS, OpenVoxTypes.DEFAULT, OpenVoxTypes.DEFINE,
        OpenVoxTypes.IF, OpenVoxTypes.ELSIF, OpenVoxTypes.ELSE, OpenVoxTypes.UNLESS,
        OpenVoxTypes.INHERITS, OpenVoxTypes.NODE, OpenVoxTypes.AND, OpenVoxTypes.OR,
        OpenVoxTypes.IN, OpenVoxTypes.UNDEF, OpenVoxTypes.TRUE, OpenVoxTypes.FALSE,
        OpenVoxTypes.FUNCTION, OpenVoxTypes.TYPE, OpenVoxTypes.ATTR, OpenVoxTypes.PRIVATE,
        OpenVoxTypes.PLAN, OpenVoxTypes.APPLY,
    )

    @JvmField
    val OPERATORS: TokenSet = TokenSet.create(
        OpenVoxTypes.EQUALS, OpenVoxTypes.APPENDS, OpenVoxTypes.DELETES, OpenVoxTypes.ISEQUAL,
        OpenVoxTypes.NOTEQUAL, OpenVoxTypes.MATCH, OpenVoxTypes.NOMATCH, OpenVoxTypes.GREATERTHAN,
        OpenVoxTypes.GREATEREQUAL, OpenVoxTypes.LESSTHAN, OpenVoxTypes.LESSEQUAL, OpenVoxTypes.FARROW,
        OpenVoxTypes.PARROW, OpenVoxTypes.LSHIFT, OpenVoxTypes.RSHIFT, OpenVoxTypes.PLUS,
        OpenVoxTypes.MINUS, OpenVoxTypes.TIMES, OpenVoxTypes.DIV, OpenVoxTypes.MODULO,
        OpenVoxTypes.NOT, OpenVoxTypes.QMARK, OpenVoxTypes.IN_EDGE, OpenVoxTypes.IN_EDGE_SUB,
        OpenVoxTypes.OUT_EDGE, OpenVoxTypes.OUT_EDGE_SUB, OpenVoxTypes.LCOLLECT,
        OpenVoxTypes.LLCOLLECT, OpenVoxTypes.RCOLLECT, OpenVoxTypes.RRCOLLECT,
    )
}
