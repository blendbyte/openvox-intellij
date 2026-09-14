package net.blendbyte.openvox.formatting

import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.TokenSet
import net.blendbyte.openvox.OpenVoxLanguage
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxFormattingModelBuilder : FormattingModelBuilder {

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val settings = formattingContext.codeStyleSettings
        val block = OpenVoxBlock(
            node = formattingContext.node,
            alignment = null,
            arrowAlignment = null,
            indent = Indent.getNoneIndent(),
            spacingBuilder = createSpacingBuilder(settings),
            settings = settings,
        )
        return FormattingModelProvider.createFormattingModelForPsiFile(
            formattingContext.containingFile, block, settings,
        )
    }
}

private val BINARY_OPERATORS = TokenSet.create(
    T.EQUALS, T.APPENDS, T.DELETES,
    T.ISEQUAL, T.NOTEQUAL, T.MATCH, T.NOMATCH,
    T.GREATERTHAN, T.GREATEREQUAL, T.LESSTHAN, T.LESSEQUAL,
    T.PLUS, T.MINUS, T.TIMES, T.DIV, T.MODULO,
    T.LSHIFT, T.RSHIFT,
    T.AND, T.OR, T.IN,
    T.IN_EDGE, T.IN_EDGE_SUB, T.OUT_EDGE, T.OUT_EDGE_SUB,
    T.FARROW, T.PARROW,
    T.QMARK,
)

internal fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
    val openVox = settings.getCustomSettings(OpenVoxCodeStyleSettings::class.java)
    return SpacingBuilder(settings, OpenVoxLanguage)
        .around(BINARY_OPERATORS).spaces(1)
        .afterInside(T.NOT, T.NOT_EXPR).none()
        .afterInside(T.MINUS, T.NEGATION_EXPR).none()
        .afterInside(T.TIMES, T.SPLAT_EXPR).none()
        .before(T.COMMA).none()
        .after(T.COMMA).spaces(1)
        .before(T.SEMIC).none()
        .after(T.SEMIC).spaces(1)
        .beforeInside(T.COLON, T.RESOURCE_BODY).none()
        .afterInside(T.COLON, T.RESOURCE_BODY).spaces(1)
        .beforeInside(T.COLON, T.CASE_OPTION).none()
        .afterInside(T.COLON, T.CASE_OPTION).spaces(1)
        .before(T.LBRACE).spaces(1)
        .before(T.SELBRACE).spaces(1)
        .between(T.RBRACE, T.ELSE).spaces(1)
        .between(T.RBRACE, T.ELSIF).spaces(1)
        .afterInside(T.LBRACE, T.HASH_LITERAL).spaceIf(openVox.SPACE_WITHIN_HASH_BRACES)
        .beforeInside(T.RBRACE, T.HASH_LITERAL).spaceIf(openVox.SPACE_WITHIN_HASH_BRACES)
        .afterInside(T.LBRACK, T.ARRAY_LITERAL).none()
        .beforeInside(T.RBRACK, T.ARRAY_LITERAL).none()
        .afterInside(T.LISTSTART, T.ARRAY_LITERAL).none()
        .withinPair(T.LBRACK, T.RBRACK).none()
        .withinPair(T.LPAREN, T.RPAREN).none()
        .withinPair(T.WSLPAREN, T.RPAREN).none()
        .afterInside(T.LBRACE, T.RESOURCE_EXPRESSION).spaces(1)
        .beforeInside(T.RBRACE, T.RESOURCE_EXPRESSION).spaces(1)
        .afterInside(T.LBRACE, T.RESOURCE_DEFAULTS_EXPRESSION).spaces(1)
        .beforeInside(T.RBRACE, T.RESOURCE_DEFAULTS_EXPRESSION).spaces(1)
        .beforeInside(T.LBRACK, T.ACCESS_EXPR).none()
        .before(T.DATA_TYPE_PARAMETERS).none()
        .before(T.ARGUMENT_LIST).none()
        .before(T.PARAMETER_LIST).spaces(1)
        .before(T.LAMBDA).spaces(1)
        .around(T.DOT).none()
}
