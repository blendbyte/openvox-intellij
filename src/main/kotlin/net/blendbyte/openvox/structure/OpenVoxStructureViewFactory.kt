package net.blendbyte.openvox.structure

import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import net.blendbyte.openvox.OpenVoxIcons
import net.blendbyte.openvox.psi.OpenVoxFile
import net.blendbyte.openvox.psi.OpenVoxTypes as T
import javax.swing.Icon

class OpenVoxStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? =
        if (psiFile !is OpenVoxFile) null
        else object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                OpenVoxStructureViewModel(psiFile)
        }
}

private class OpenVoxStructureViewModel(file: OpenVoxFile) :
    StructureViewModelBase(file, OpenVoxStructureElement(file)),
    StructureViewModel.ElementInfoProvider {

    override fun getSorters() = arrayOf(Sorter.ALPHA_SORTER)
    override fun isAlwaysShowsPlus(element: StructureViewTreeElement) = false
    override fun isAlwaysLeaf(element: StructureViewTreeElement) = false
}

private class OpenVoxStructureElement(private val element: PsiElement) :
    StructureViewTreeElement, SortableTreeElement {

    override fun getValue(): Any = element
    override fun navigate(requestFocus: Boolean) = (element as? com.intellij.pom.Navigatable)?.navigate(requestFocus) ?: Unit
    override fun canNavigate(): Boolean = (element as? com.intellij.pom.Navigatable)?.canNavigate() ?: false
    override fun canNavigateToSource(): Boolean = canNavigate()
    override fun getAlphaSortKey(): String = presentableName() ?: ""

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = presentableName()
        override fun getLocationString(): String? = null
        override fun getIcon(unused: Boolean): Icon? = iconFor()
    }

    override fun getChildren(): Array<StructureViewTreeElement> {
        val scope = when {
            element is PsiFile -> element
            element.node.elementType in DEFINITIONS ->
                element.node.findChildByType(T.BLOCK)?.psi ?: return emptyArray()
            else -> return emptyArray()
        }
        return scope.children
            .flatMap { child -> collectInteresting(child) }
            .map { OpenVoxStructureElement(it) }
            .toTypedArray()
    }

    private fun collectInteresting(child: PsiElement): List<PsiElement> =
        if (child.node?.elementType in INTERESTING) listOf(child)
        else child.children.filter { it.node?.elementType in INTERESTING }

    private fun presentableName(): String? = when {
        element is PsiFile -> element.name
        element.node.elementType == T.RESOURCE_EXPRESSION -> element.text.substringBefore('{').trim().ifEmpty { "resource" }
        else -> (element as? PsiNamedElement)?.name ?: element.firstChild?.nextSibling?.text
    }

    private fun iconFor(): Icon? = OpenVoxIcons.FILE

    private companion object {
        val DEFINITIONS = setOf(T.CLASS_DEFINITION, T.DEFINE_DEFINITION, T.FUNCTION_DEFINITION, T.PLAN_DEFINITION, T.NODE_DEFINITION)
        val INTERESTING = setOf(
            T.CLASS_DEFINITION, T.DEFINE_DEFINITION, T.FUNCTION_DEFINITION,
            T.TYPE_ALIAS, T.PLAN_DEFINITION, T.NODE_DEFINITION, T.RESOURCE_EXPRESSION,
        )
    }
}
