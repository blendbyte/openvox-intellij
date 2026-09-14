package net.blendbyte.openvox.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxFoldingBuilder : FoldingBuilderEx(), DumbAware {

    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()

        PsiTreeUtil.findChildrenOfAnyType(root, PsiElement::class.java).forEach { element ->
            val node = element.node ?: return@forEach
            when (node.elementType) {
                T.BLOCK, T.HASH_LITERAL, T.ARRAY_LITERAL, T.PARAMETER_LIST, T.ATTRIBUTE_OPERATIONS ->
                    addIfMultiline(descriptors, node, document)
                T.BLOCK_COMMENT ->
                    addIfMultiline(descriptors, node, document)
                else -> Unit
            }
        }

        var cursor: ASTNode? = root.node?.firstChildNode
        var bodyStart: ASTNode? = null
        while (cursor != null) {
            cursor = nextLeaf(cursor)
            val type = cursor?.elementType ?: break
            if (type == T.HD_TEXT && bodyStart == null) bodyStart = cursor
            if (type == T.HD_END && bodyStart != null) {
                val range = TextRange(bodyStart.startOffset, cursor!!.textRange.endOffset)
                if (spansLines(range, document)) {
                    descriptors += FoldingDescriptor(cursor!!.treeParent ?: cursor!!, range, null, "…")
                }
                bodyStart = null
            }
        }

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String = when (node.elementType) {
        T.HASH_LITERAL -> "{…}"
        T.ARRAY_LITERAL -> "[…]"
        T.PARAMETER_LIST -> "(…)"
        T.BLOCK_COMMENT -> "/*…*/"
        else -> "{…}"
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun addIfMultiline(into: MutableList<FoldingDescriptor>, node: ASTNode, document: Document) {
        val range = node.textRange
        if (spansLines(range, document)) into += FoldingDescriptor(node, range)
    }

    private fun spansLines(range: TextRange, document: Document): Boolean =
        range.length > 0 &&
            range.endOffset <= document.textLength &&
            document.getLineNumber(range.startOffset) < document.getLineNumber(range.endOffset)

    private fun nextLeaf(node: ASTNode): ASTNode? {
        node.firstChildNode?.let { return it }
        var current: ASTNode? = node
        while (current != null) {
            current.treeNext?.let { return it }
            current = current.treeParent
        }
        return null
    }
}
