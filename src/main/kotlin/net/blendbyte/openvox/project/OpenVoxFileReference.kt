package net.blendbyte.openvox.project

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

class OpenVoxFileReference(
    element: PsiElement,
    range: TextRange,
    private val subdirectory: String,
) : PsiPolyVariantReferenceBase<PsiElement>(element, range) {

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val path = rangeInElement.substring(element.text).trim()
        if (path.isEmpty()) return ResolveResult.EMPTY_ARRAY

        val segments = path.removePrefix("puppet:///modules/").split('/')
        if (segments.size < 2) return ResolveResult.EMPTY_ARRAY

        val module = segments.first()
        val rest = segments.drop(1).joinToString("/")
        val expected = "/$module/$subdirectory/$rest"

        val project = element.project
        val manager = PsiManager.getInstance(project)
        val candidates = FilenameIndex.getVirtualFilesByName(
            rest.substringAfterLast('/'),
            GlobalSearchScope.allScope(project),
        )
        return candidates
            .filter { it.path.replace('\\', '/').endsWith(expected) }
            .mapNotNull { manager.findFile(it) }
            .let { PsiElementResolveResult.createResults(it) }
    }
}
