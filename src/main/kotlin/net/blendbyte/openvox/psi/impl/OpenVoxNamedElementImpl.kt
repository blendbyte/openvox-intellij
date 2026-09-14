package net.blendbyte.openvox.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.tree.TokenSet
import com.intellij.util.IncorrectOperationException
import net.blendbyte.openvox.psi.OpenVoxNamedElement
import net.blendbyte.openvox.resolve.OpenVoxElementFactory
import net.blendbyte.openvox.resolve.OpenVoxScope
import net.blendbyte.openvox.psi.OpenVoxTypes as T

abstract class OpenVoxNamedElementImpl(node: ASTNode) : OpenVoxPsiElement(node), OpenVoxNamedElement {
    override fun getNameIdentifier(): PsiElement? {
        var current = node
        while (true) {
            current.findChildByType(NAME_TOKENS)?.let { return it.psi }
            current = current.findChildByType(NAME_WRAPPERS) ?: return null
        }
    }

    override fun getName(): String? = nameIdentifier?.text?.removePrefix("$")

    override fun setName(name: String): PsiElement {
        val identifier = nameIdentifier ?: throw IncorrectOperationException("This declaration has no name to change")
        OpenVoxElementFactory.renameToken(identifier, name)
        return this
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    override fun getUseScope(): SearchScope =
        if (node.elementType == T.PARAMETER || node.elementType == T.ASSIGN_EXPR) {
            LocalSearchScope(OpenVoxScope.ownerOf(parent) ?: containingFile)
        } else super.getUseScope()

    private companion object {
        val NAME_TOKENS = TokenSet.create(T.IDENTIFIER, T.BARE_WORD, T.CLASSREF, T.VARIABLE, T.SQ_STRING)
        val NAME_WRAPPERS = TokenSet.create(T.QUALIFIED_NAME, T.DATA_TYPE, T.TYPE_REFERENCE, T.VARIABLE_EXPR)
    }
}
