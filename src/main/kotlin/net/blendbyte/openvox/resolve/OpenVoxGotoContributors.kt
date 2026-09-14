package net.blendbyte.openvox.resolve

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import net.blendbyte.openvox.index.OpenVoxDeclarationKind

class OpenVoxGotoClassContributor : OpenVoxGotoContributorBase(
    listOf(
        OpenVoxDeclarationKind.CLASS,
        OpenVoxDeclarationKind.DEFINE,
        OpenVoxDeclarationKind.PLAN,
        OpenVoxDeclarationKind.NODE,
    ),
)

class OpenVoxGotoSymbolContributor : OpenVoxGotoContributorBase(OpenVoxDeclarationKind.entries)

abstract class OpenVoxGotoContributorBase(private val kinds: List<OpenVoxDeclarationKind>) :
    ChooseByNameContributorEx {

    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        val project = scope.project ?: return
        kinds.asSequence()
            .flatMap { OpenVoxResolver.allNames(project, it).asSequence() }
            .distinct()
            .forEach { if (!processor.process(it)) return }
    }

    override fun processElementsWithName(
        name: String,
        processor: Processor<in NavigationItem>,
        parameters: FindSymbolParameters,
    ) {
        val project = parameters.project
        kinds.asSequence()
            .flatMap { OpenVoxResolver.find(project, it, name, parameters.searchScope).asSequence() }
            .filterIsInstance<NavigationItem>()
            .forEach { if (!processor.process(it)) return }
    }
}
