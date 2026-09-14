package net.blendbyte.openvox.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as D
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import net.blendbyte.openvox.lexer.EppLexerAdapter
import net.blendbyte.openvox.lexer.OpenVoxLexerAdapter
import net.blendbyte.openvox.psi.OpenVoxTokenSets
import net.blendbyte.openvox.psi.OpenVoxTypes as T

object OpenVoxColors {
    val KEYWORD = createTextAttributesKey("OPENVOX_KEYWORD", D.KEYWORD)
    val COMMENT = createTextAttributesKey("OPENVOX_COMMENT", D.LINE_COMMENT)
    val STRING = createTextAttributesKey("OPENVOX_STRING", D.STRING)
    val ESCAPE = createTextAttributesKey("OPENVOX_ESCAPE", D.VALID_STRING_ESCAPE)
    val NUMBER = createTextAttributesKey("OPENVOX_NUMBER", D.NUMBER)
    val VARIABLE = createTextAttributesKey("OPENVOX_VARIABLE", D.INSTANCE_FIELD)
    val DATA_TYPE = createTextAttributesKey("OPENVOX_DATA_TYPE", D.CLASS_NAME)
    val IDENTIFIER = createTextAttributesKey("OPENVOX_IDENTIFIER", D.IDENTIFIER)
    val OPERATOR = createTextAttributesKey("OPENVOX_OPERATOR", D.OPERATION_SIGN)
    val BRACES = createTextAttributesKey("OPENVOX_BRACES", D.BRACES)
    val BRACKETS = createTextAttributesKey("OPENVOX_BRACKETS", D.BRACKETS)
    val PARENTHESES = createTextAttributesKey("OPENVOX_PARENTHESES", D.PARENTHESES)
    val COMMA_SEMI = createTextAttributesKey("OPENVOX_COMMA", D.COMMA)
    val REGEX = createTextAttributesKey("OPENVOX_REGEX", D.STRING)
    val INTERPOLATION = createTextAttributesKey("OPENVOX_INTERPOLATION", D.TEMPLATE_LANGUAGE_COLOR)
    val EPP_TAG = createTextAttributesKey("OPENVOX_EPP_TAG", D.METADATA)
    val EPP_TEXT = createTextAttributesKey("OPENVOX_EPP_TEXT", D.TEMPLATE_LANGUAGE_COLOR)
    val BAD_CHARACTER = createTextAttributesKey("OPENVOX_BAD_CHARACTER", com.intellij.openapi.editor.HighlighterColors.BAD_CHARACTER)
}

open class OpenVoxSyntaxHighlighter(private val epp: Boolean = false) : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = if (epp) EppLexerAdapter() else OpenVoxLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(
        when (tokenType) {
            T.SQ_STRING, T.DQ_START, T.DQ_END, T.DQ_TEXT, T.HD_TEXT, T.HD_TAG, T.HD_END -> OpenVoxColors.STRING
            T.DQ_ESCAPE -> OpenVoxColors.ESCAPE
            T.INTERP_START, T.INTERP_END -> OpenVoxColors.INTERPOLATION
            T.EPP_START, T.EPP_EXPR_START, T.EPP_START_TRIM, T.EPP_END, T.EPP_END_TRIM -> OpenVoxColors.EPP_TAG
            T.EPP_TEXT -> OpenVoxColors.EPP_TEXT
            T.NUMBER -> OpenVoxColors.NUMBER
            T.VARIABLE -> OpenVoxColors.VARIABLE
            T.CLASSREF -> OpenVoxColors.DATA_TYPE
            T.REGEX -> OpenVoxColors.REGEX
            T.IDENTIFIER, T.BARE_WORD -> OpenVoxColors.IDENTIFIER
            T.LBRACE, T.RBRACE, T.SELBRACE -> OpenVoxColors.BRACES
            T.LBRACK, T.LISTSTART, T.RBRACK -> OpenVoxColors.BRACKETS
            T.LPAREN, T.WSLPAREN, T.RPAREN -> OpenVoxColors.PARENTHESES
            T.COMMA, T.SEMIC, T.COLON -> OpenVoxColors.COMMA_SEMI
            TokenType.BAD_CHARACTER -> OpenVoxColors.BAD_CHARACTER
            in OpenVoxTokenSets.KEYWORDS -> OpenVoxColors.KEYWORD
            in OpenVoxTokenSets.COMMENTS -> OpenVoxColors.COMMENT
            in OpenVoxTokenSets.OPERATORS -> OpenVoxColors.OPERATOR
            else -> null
        }
    )
}

class EppSyntaxHighlighter : OpenVoxSyntaxHighlighter(epp = true)

class OpenVoxSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        OpenVoxSyntaxHighlighter()
}

class EppSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        EppSyntaxHighlighter()
}
