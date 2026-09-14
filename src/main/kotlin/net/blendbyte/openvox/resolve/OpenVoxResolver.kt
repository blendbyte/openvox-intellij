package net.blendbyte.openvox.resolve

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import net.blendbyte.openvox.index.OpenVoxDeclarationIndex
import net.blendbyte.openvox.index.OpenVoxDeclarationKind
import net.blendbyte.openvox.psi.OpenVoxNamedElement

object OpenVoxResolver {

    fun find(
        project: Project,
        kind: OpenVoxDeclarationKind,
        name: String,
        scope: GlobalSearchScope = GlobalSearchScope.allScope(project),
    ): List<OpenVoxNamedElement> {
        val normalised = name.removePrefix("::").lowercase()
        val key = OpenVoxDeclarationIndex.keyFor(kind, normalised)
        val index = FileBasedIndex.getInstance()
        val manager = PsiManager.getInstance(project)

        return index.getContainingFiles(OpenVoxDeclarationIndex.KEY, key, scope)
            .mapNotNull { manager.findFile(it) }
            .flatMap { OpenVoxDeclarationIndex.declarationsIn(it) }
            .filter {
                OpenVoxDeclarationKind.of(it) == kind &&
                    it.name?.removePrefix("::")?.lowercase() == normalised
            }
    }

    fun findAny(project: Project, name: String, scope: GlobalSearchScope = GlobalSearchScope.allScope(project)) =
        OpenVoxDeclarationKind.entries.flatMap { find(project, it, name, scope) }

    fun allNames(project: Project, kind: OpenVoxDeclarationKind): List<String> {
        val prefix = "${kind.prefix}:"
        val names = mutableListOf<String>()
        FileBasedIndex.getInstance().processAllKeys(
            OpenVoxDeclarationIndex.KEY,
            { key -> if (key.startsWith(prefix)) names += key.removePrefix(prefix); true },
            project,
        )
        return names
    }
}
