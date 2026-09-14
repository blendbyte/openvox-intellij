package net.blendbyte.openvox.project

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import net.blendbyte.openvox.psi.OpenVoxLiteralExpr
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxProjectReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(OpenVoxLiteralExpr::class.java),
            FilePathProvider,
        )
    }
}

private object FilePathProvider : PsiReferenceProvider() {

    private val TEMPLATE_FUNCTIONS = setOf("epp", "template", "inline_epp", "inline_template")
    private val FILE_ATTRIBUTES = setOf("source")

    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val quoted = element.firstChild?.takeIf { it.node?.elementType == T.SQ_STRING } ?: return PsiReference.EMPTY_ARRAY
        val text = quoted.text
        if (text.length < 3) return PsiReference.EMPTY_ARRAY
        val range = TextRange(1, text.length - 1)

        callName(element)?.let { name ->
            if (name in TEMPLATE_FUNCTIONS) {
                return arrayOf(OpenVoxFileReference(element, range, "templates"))
            }
        }
        attributeName(element)?.let { name ->
            if (name in FILE_ATTRIBUTES) {
                return arrayOf(OpenVoxFileReference(element, range, "files"))
            }
        }
        return PsiReference.EMPTY_ARRAY
    }

    private fun callName(element: PsiElement): String? {
        val argumentList = generateSequence(element) { it.parent }
            .firstOrNull { it.node?.elementType == T.ARGUMENT_LIST } ?: return null
        val call = argumentList.parent?.takeIf { it.node?.elementType == T.FUNCTION_CALL_EXPR } ?: return null
        return call.firstChild?.text
    }

    private fun attributeName(element: PsiElement): String? {
        val operation = generateSequence(element) { it.parent }
            .firstOrNull { it.node?.elementType == T.ATTRIBUTE_OPERATION } ?: return null
        return operation.firstChild?.text
    }
}
