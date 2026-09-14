package net.blendbyte.openvox.formatting

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxBlock(
    node: ASTNode,
    alignment: Alignment?,
    private val arrowAlignment: Alignment?,
    private val indent: Indent,
    private val spacingBuilder: SpacingBuilder,
    private val settings: CodeStyleSettings,
) : AbstractBlock(node, null, alignment) {

    private val openVox get() = settings.getCustomSettings(OpenVoxCodeStyleSettings::class.java)

    override fun getIndent(): Indent = indent

    override fun isLeaf(): Boolean = node.firstChildNode == null

    override fun getSpacing(child1: Block?, child2: Block): Spacing? =
        spacingBuilder.getSpacing(this, child1, child2)

    override fun buildChildren(): List<Block> {
        val parentType = node.elementType
        val group = alignmentGroupFor(parentType)
        val children = mutableListOf<Block>()

        var child: ASTNode? = node.firstChildNode
        while (child != null) {
            val childType = child.elementType
            if (childType != TokenType.WHITE_SPACE && child.textLength > 0) {
                children += OpenVoxBlock(
                    node = child,
                    alignment = if (childType == T.FARROW || childType == T.PARROW) arrowAlignment else null,
                    arrowAlignment = if (ENTRY_TYPES.contains(childType)) group else arrowAlignment,
                    indent = indentFor(childType, parentType),
                    spacingBuilder = spacingBuilder,
                    settings = settings,
                )
            }
            child = child.treeNext
        }
        return children
    }

    private fun alignmentGroupFor(type: IElementType): Alignment? = when (type) {
        T.ATTRIBUTE_OPERATIONS -> if (openVox.ALIGN_RESOURCE_ARROWS) Alignment.createAlignment(true) else null
        T.HASH_LITERAL -> if (openVox.ALIGN_HASH_ARROWS) Alignment.createAlignment(true) else null
        T.SELECTOR_EXPR -> if (openVox.ALIGN_SELECTOR_ARROWS) Alignment.createAlignment(true) else null
        else -> null
    }

    private fun indentFor(childType: IElementType, parentType: IElementType): Indent {
        if (BRACKETS.contains(childType)) return Indent.getNoneIndent()
        return when (parentType) {
            T.BLOCK, T.HASH_LITERAL, T.ARRAY_LITERAL, T.PARAMETER_LIST, T.ARGUMENT_LIST,
            T.DATA_TYPE_PARAMETERS, T.EPP_PARAMETERS,
            -> Indent.getNormalIndent()

            T.RESOURCE_EXPRESSION, T.RESOURCE_DEFAULTS_EXPRESSION ->
                if (childType == T.RESOURCE_BODY || childType == T.ATTRIBUTE_OPERATIONS) Indent.getNormalIndent()
                else Indent.getNoneIndent()

            T.ATTRIBUTE_OPERATIONS -> Indent.getNoneIndent()

            T.CASE_EXPRESSION -> if (childType == T.CASE_OPTION) Indent.getNormalIndent() else Indent.getNoneIndent()
            T.SELECTOR_EXPR -> if (childType == T.SELECTOR_ENTRY) Indent.getNormalIndent() else Indent.getNoneIndent()
            T.RESOURCE_BODY -> if (childType == T.ATTRIBUTE_OPERATIONS) Indent.getNormalIndent() else Indent.getNoneIndent()

            else -> Indent.getNoneIndent()
        }
    }

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val childIndent = when (node.elementType) {
            T.BLOCK, T.HASH_LITERAL, T.ARRAY_LITERAL, T.PARAMETER_LIST, T.ARGUMENT_LIST,
            T.DATA_TYPE_PARAMETERS, T.EPP_PARAMETERS, T.ATTRIBUTE_OPERATIONS,
            T.RESOURCE_EXPRESSION, T.RESOURCE_DEFAULTS_EXPRESSION, T.RESOURCE_BODY,
            T.CASE_EXPRESSION, T.SELECTOR_EXPR,
            -> Indent.getNormalIndent()
            else -> Indent.getNoneIndent()
        }
        return ChildAttributes(childIndent, null)
    }

    private companion object {
        val BRACKETS: TokenSet = TokenSet.create(
            T.LBRACE, T.RBRACE, T.SELBRACE,
            T.LBRACK, T.LISTSTART, T.RBRACK,
            T.LPAREN, T.WSLPAREN, T.RPAREN,
        )
        val ENTRY_TYPES: TokenSet = TokenSet.create(
            T.ATTRIBUTE_OPERATION, T.HASH_ENTRY, T.SELECTOR_ENTRY,
        )
    }
}
