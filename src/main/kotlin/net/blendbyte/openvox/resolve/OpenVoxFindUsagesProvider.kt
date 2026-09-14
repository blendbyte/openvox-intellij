package net.blendbyte.openvox.resolve

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.tree.TokenSet
import net.blendbyte.openvox.index.OpenVoxDeclarationKind
import net.blendbyte.openvox.lexer.OpenVoxLexerAdapter
import net.blendbyte.openvox.psi.OpenVoxTokenSets
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxFindUsagesProvider : FindUsagesProvider {

    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        OpenVoxLexerAdapter(),
        TokenSet.create(T.IDENTIFIER, T.BARE_WORD, T.CLASSREF, T.VARIABLE),
        OpenVoxTokenSets.COMMENTS,
        OpenVoxTokenSets.STRINGS,
    )

    override fun canFindUsagesFor(element: PsiElement): Boolean = element is PsiNamedElement

    override fun getHelpId(element: PsiElement): String = HelpID.FIND_OTHER_USAGES

    override fun getType(element: PsiElement): String =
        OpenVoxDeclarationKind.of(element)?.presentableName
            ?: if (element.node?.elementType == T.PARAMETER) "parameter" else "declaration"

    override fun getDescriptiveName(element: PsiElement): String =
        (element as? PsiNamedElement)?.name ?: element.text

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String =
        getDescriptiveName(element)
}
