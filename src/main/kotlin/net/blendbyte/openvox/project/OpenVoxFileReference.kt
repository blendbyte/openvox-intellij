package net.blendbyte.openvox.project

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.FilenameIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
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
        val suffix = "/$subdirectory/$rest"

        val project = element.project
        val manager = PsiManager.getInstance(project)
        val candidates = FilenameIndex.getVirtualFilesByName(
            rest.substringAfterLast('/'),
            GlobalSearchScope.allScope(project),
        )
        return candidates
            .filter { file ->
                val path = file.path.replace('\\', '/')
                path.endsWith(suffix) && isModule(file, path.length - suffix.length, module)
            }
            .mapNotNull { manager.findFile(it) }
            .let { PsiElementResolveResult.createResults(it) }
    }

    /**
     * Whether the directory that holds [subdirectory] is [module]. A module checked out as its own
     * repository is often named `puppetlabs-apache` or `puppet-apache`; its metadata.json decides.
     */
    private fun isModule(file: VirtualFile, rootLength: Int, module: String): Boolean {
        val root = generateSequence(file.parent) { it.parent }
            .firstOrNull { it.path.replace('\\', '/').length == rootLength } ?: return false
        if (root.name == module) return true
        val metadata = root.findChild("metadata.json") ?: return false
        val name = METADATA_NAME.find(VfsUtilCore.loadText(metadata))?.groupValues?.get(1) ?: return false
        return name.substringAfterLast('/').substringAfterLast('-') == module
    }

    private companion object {
        val METADATA_NAME = Regex(""""name"\s*:\s*"([^"]+)"""")
    }
}
