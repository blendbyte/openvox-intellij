package net.blendbyte.openvox.resolve

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import net.blendbyte.openvox.psi.OpenVoxNamedElement
import net.blendbyte.openvox.psi.OpenVoxTypes as T

object OpenVoxScope {
    private val SCOPE_OWNERS = setOf(
        T.CLASS_DEFINITION, T.DEFINE_DEFINITION, T.FUNCTION_DEFINITION,
        T.PLAN_DEFINITION, T.NODE_DEFINITION, T.LAMBDA, T.EPP_TEMPLATE,
    )

    fun normalise(name: String): String = name.removePrefix("$").removePrefix("::")

    fun ownerOf(element: PsiElement?): PsiElement? =
        generateSequence(element) { it.parent }.firstOrNull { it is PsiFile || it.node?.elementType in SCOPE_OWNERS }

    fun resolve(usage: PsiElement, name: String): OpenVoxNamedElement? {
        val assignment = usage.parent
        if (assignment?.node?.elementType == T.ASSIGN_EXPR && assignment.firstChild === usage) {
            return assignment as? OpenVoxNamedElement
        }
        val wanted = normalise(name)
        val topScope = usage.text.removePrefix("$").startsWith("::")
        for (scope in scopesAt(usage)) {
            if (topScope && scope !is PsiFile) continue
            declarationsIn(scope).byName[wanted]?.lastOrNull { it.textRange.endOffset <= usage.textOffset }?.let { return it }
        }
        return null
    }

    fun declarationsVisibleFrom(usage: PsiElement): List<OpenVoxNamedElement> =
        scopesAt(usage)
            .flatMap { declarationsIn(it).all.asSequence() }
            .filter { it.textRange.endOffset <= usage.textOffset }
            .toList()

    private fun scopesAt(usage: PsiElement): Sequence<PsiElement> =
        generateSequence(ownerOf(usage.parent)) { if (it is PsiFile) null else ownerOf(it.parent) }

    private data class Declarations(val all: List<OpenVoxNamedElement>) {
        val byName = all.groupBy { it.name }
    }

    private fun declarationsIn(scope: PsiElement): Declarations =
        CachedValuesManager.getCachedValue(scope) {
            val declarations = mutableListOf<OpenVoxNamedElement>()
            scope.acceptChildren(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    ProgressManager.checkCanceled()
                    if (element.node.elementType in SCOPE_OWNERS) return
                    if (element is OpenVoxNamedElement && element.nameIdentifier?.node?.elementType == T.VARIABLE) {
                        declarations += element
                    }
                    super.visitElement(element)
                }
            })
            CachedValueProvider.Result.create(Declarations(declarations), scope)
        }
}
