package net.blendbyte.openvox.resolve

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import net.blendbyte.openvox.psi.OpenVoxNamedElement
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxReferenceSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val declaration = parameters.elementToSearch as? OpenVoxNamedElement ?: return
        val name = declaration.name ?: return
        val variable = declaration.nameIdentifier?.node?.elementType == T.VARIABLE
        parameters.optimizer.searchWord(name, parameters.effectiveSearchScope, UsageSearchContext.ANY, variable, declaration)
    }
}
