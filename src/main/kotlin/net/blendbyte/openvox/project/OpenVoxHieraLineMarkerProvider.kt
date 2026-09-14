package net.blendbyte.openvox.project

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import net.blendbyte.openvox.OpenVoxIcons
import net.blendbyte.openvox.index.OpenVoxDeclarationKind
import net.blendbyte.openvox.index.OpenVoxHieraIndex
import net.blendbyte.openvox.psi.OpenVoxNamedElement
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxHieraLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<*>>) {
        if (element.node.elementType != T.VARIABLE || element.parent.node.elementType != T.PARAMETER) return
        val owner = generateSequence(element.parent.parent) { it.parent }
            .filterIsInstance<OpenVoxNamedElement>()
            .firstOrNull { OpenVoxDeclarationKind.of(it) != null } ?: return
        if (OpenVoxDeclarationKind.of(owner) !in setOf(OpenVoxDeclarationKind.CLASS, OpenVoxDeclarationKind.DEFINE)) return
        val key = "${owner.name}::${element.text.removePrefix("$")}"
        val project = element.project
        val manager = PsiManager.getInstance(project)
        val targets = FileBasedIndex.getInstance().getContainingFiles(OpenVoxHieraIndex.KEY, key, GlobalSearchScope.allScope(project))
            .asSequence()
            .filter { "/data/" in it.path.replace('\\', '/') }
            .take(20)
            .mapNotNull(manager::findFile)
            .toList()
        if (targets.isEmpty()) return
        result += NavigationGutterIconBuilder.create(OpenVoxIcons.FILE)
            .setTargets(targets)
            .setTooltipText("Set in Hiera data as $key")
            .createLineMarkerInfo(element)
    }
}
