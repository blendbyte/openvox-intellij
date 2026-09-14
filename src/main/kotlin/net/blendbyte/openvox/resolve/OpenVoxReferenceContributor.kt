package net.blendbyte.openvox.resolve

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import net.blendbyte.openvox.index.OpenVoxDeclarationKind
import net.blendbyte.openvox.psi.OpenVoxQualifiedName
import net.blendbyte.openvox.psi.OpenVoxResourceType
import net.blendbyte.openvox.psi.OpenVoxTypeReference
import net.blendbyte.openvox.psi.OpenVoxVariableExpr
import net.blendbyte.openvox.psi.OpenVoxWordRef
import net.blendbyte.openvox.psi.OpenVoxLiteralExpr
import net.blendbyte.openvox.psi.OpenVoxDqString

class OpenVoxReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(OpenVoxLiteralExpr::class.java), QuotedClassProvider)
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(OpenVoxDqString::class.java), QuotedClassProvider)
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(OpenVoxWordRef::class.java),
            NameProvider,
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(OpenVoxQualifiedName::class.java),
            NameProvider,
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(OpenVoxResourceType::class.java),
            ResourceTypeProvider,
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(OpenVoxTypeReference::class.java),
            TypeReferenceProvider,
        )
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(OpenVoxVariableExpr::class.java),
            VariableProvider,
        )
    }
}

private object QuotedClassProvider : PsiReferenceProvider() {
    private val CLASS_NAME = Regex("(?:::)?[a-z][\\w]*(?:::[a-z][\\w]*)*")
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val isClassCall = OpenVoxReferenceContext.statementCallName(element) in OpenVoxReferenceContext.CLASS_CALLS ||
            OpenVoxReferenceContext.parenthesisedCallName(element) in OpenVoxReferenceContext.CLASS_CALLS
        if (!isClassCall) return PsiReference.EMPTY_ARRAY
        val text = element.text
        if (text.length < 3 || text.first() != text.last() || text.first() !in "'\"") return PsiReference.EMPTY_ARRAY
        if (!CLASS_NAME.matches(text.substring(1, text.lastIndex))) {
            return PsiReference.EMPTY_ARRAY
        }
        return arrayOf(OpenVoxDeclarationReference(element, TextRange(1, text.lastIndex), listOf(OpenVoxDeclarationKind.CLASS)))
    }
}

private object NameProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        if (OpenVoxReferenceContext.isDeclarationName(element)) return PsiReference.EMPTY_ARRAY
        val range = TextRange(0, element.textLength)
        if (OpenVoxReferenceContext.isInterpolationVariable(element)) {
            return arrayOf(OpenVoxVariableReference(element, range))
        }

        val statementCall = OpenVoxReferenceContext.statementCallName(element)
        if (statementCall in OpenVoxReferenceContext.CLASS_CALLS) {
            return arrayOf(OpenVoxDeclarationReference(element, range, listOf(OpenVoxDeclarationKind.CLASS)))
        }
        val parenCall = OpenVoxReferenceContext.parenthesisedCallName(element)
        if (parenCall in OpenVoxReferenceContext.CLASS_CALLS) {
            return arrayOf(OpenVoxDeclarationReference(element, range, listOf(OpenVoxDeclarationKind.CLASS)))
        }
        if (OpenVoxReferenceContext.isCalleeName(element)) {
            return arrayOf(OpenVoxDeclarationReference(element, range, listOf(OpenVoxDeclarationKind.FUNCTION)))
        }
        if (element.parent?.node?.elementType == net.blendbyte.openvox.psi.OpenVoxTypes.INHERITS_CLAUSE) {
            return arrayOf(OpenVoxDeclarationReference(element, range, listOf(OpenVoxDeclarationKind.CLASS)))
        }
        return PsiReference.EMPTY_ARRAY
    }
}

private object ResourceTypeProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
        arrayOf(
            OpenVoxDeclarationReference(
                element,
                TextRange(0, element.textLength),
                listOf(OpenVoxDeclarationKind.DEFINE, OpenVoxDeclarationKind.CLASS),
            ),
        )
}

private object TypeReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
        arrayOf(
            OpenVoxDeclarationReference(
                element,
                TextRange(0, element.textLength),
                listOf(
                    OpenVoxDeclarationKind.TYPE_ALIAS,
                    OpenVoxDeclarationKind.DEFINE,
                    OpenVoxDeclarationKind.CLASS,
                ),
            ),
        )
}

private object VariableProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> =
        arrayOf(OpenVoxVariableReference(element, TextRange(0, element.textLength)))
}
