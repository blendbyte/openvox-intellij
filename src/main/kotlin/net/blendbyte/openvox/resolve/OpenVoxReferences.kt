package net.blendbyte.openvox.resolve

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.ResolveResult
import net.blendbyte.openvox.index.OpenVoxDeclarationKind
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxDeclarationReference(
    element: PsiElement,
    range: TextRange,
    private val kinds: List<OpenVoxDeclarationKind>,
) : PsiPolyVariantReferenceBase<PsiElement>(element, range) {

    private val name: String get() = rangeInElement.substring(element.text)

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        for (kind in kinds) {
            val found = OpenVoxResolver.find(project, kind, name)
            if (found.isNotEmpty()) return PsiElementResolveResult.createResults(found)
        }
        return ResolveResult.EMPTY_ARRAY
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val identifier = element.node.findChildByType(T.DQ_TEXT)?.psi ?: element.firstChild ?: return element
        val bare = newElementName.removePrefix("::")
        val name = if (identifier.node.elementType == T.CLASSREF) {
            bare.split("::").joinToString("::") { it.replaceFirstChar(Char::uppercaseChar) }
        } else bare
        val qualified = if (this.name.startsWith("::")) "::$name" else name
        OpenVoxElementFactory.renameToken(identifier, qualified)
        return element
    }

    override fun getVariants(): Array<Any> =
        kinds.flatMap { OpenVoxResolver.allNames(element.project, it) }.distinct().toTypedArray()
}

class OpenVoxVariableReference(element: PsiElement, range: TextRange) :
    PsiPolyVariantReferenceBase<PsiElement>(element, range) {

    private val name: String get() = OpenVoxScope.normalise(rangeInElement.substring(element.text))

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val target = OpenVoxScope.resolve(element, name) ?: return ResolveResult.EMPTY_ARRAY
        return arrayOf(PsiElementResolveResult(target))
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        val variable = element.firstChild ?: return element
        val prefix = if (variable.text.removePrefix("$").startsWith("::")) "::" else ""
        OpenVoxElementFactory.renameToken(variable, prefix + OpenVoxScope.normalise(newElementName))
        return element
    }

    override fun getVariants(): Array<Any> =
        OpenVoxScope.declarationsVisibleFrom(element).mapNotNull { it.name }.distinct().toTypedArray()
}

object OpenVoxReferenceContext {

    private val DECLARATION_OWNERS = setOf(
        T.CLASS_DEFINITION, T.DEFINE_DEFINITION, T.FUNCTION_DEFINITION,
        T.PLAN_DEFINITION, T.NODE_DEFINITION, T.TYPE_ALIAS,
    )

    fun isDeclarationName(element: PsiElement): Boolean {
        val parent = element.parent ?: return false
        if (parent.node?.elementType !in DECLARATION_OWNERS) return false
        return parent.children.firstOrNull { it.node?.elementType != T.CLASS } === element ||
            parent.children.firstOrNull() === element
    }

    fun statementCallName(element: PsiElement): String? {
        val call = generateSequence(element.parent) { it.parent }
            .firstOrNull { it.node?.elementType in setOf(T.BARE_FUNCTION_CALL, T.FUNCTION_CALL_EXPR, T.LAMBDA) }
        return call?.takeIf { it.node.elementType == T.BARE_FUNCTION_CALL }?.firstChild?.text
    }

    fun parenthesisedCallName(element: PsiElement): String? {
        val argumentList = generateSequence(element) { it.parent }
            .firstOrNull { it.node?.elementType == T.ARGUMENT_LIST } ?: return null
        val call = argumentList.parent?.takeIf { it.node?.elementType == T.FUNCTION_CALL_EXPR } ?: return null
        return call.firstChild?.text
    }

    fun isCalleeName(element: PsiElement): Boolean {
        val parent = element.parent ?: return false
        return parent.node?.elementType == T.FUNCTION_CALL_EXPR && parent.firstChild === element
    }

    fun isInterpolationVariable(element: PsiElement): Boolean {
        if (element.node.elementType != T.WORD_REF) return false
        var current = element
        while (current.parent?.node?.elementType in setOf(T.ACCESS_EXPR, T.METHOD_CALL_EXPR)) {
            if (current.parent.firstChild !== current) return false
            current = current.parent
        }
        return current.parent?.node?.elementType == T.INTERPOLATION
    }

    /** Whether [element] is the title of a `class { 'name': }` resource body. */
    fun isClassResourceTitle(element: PsiElement): Boolean {
        val body = element.parent?.takeIf { it.node?.elementType == T.RESOURCE_BODY && it.firstChild === element }
            ?: return false
        val type = body.parent?.node?.findChildByType(T.RESOURCE_TYPE) ?: return false
        return type.findChildByType(T.CLASS) != null
    }

    /** The class named by the `class { 'name': }` body that contains [element], if any. */
    fun classResourceName(element: PsiElement): String? {
        val body = generateSequence(element) { it.parent }.firstOrNull { it.node?.elementType == T.RESOURCE_BODY }
            ?: return null
        val title = body.firstChild?.takeIf(::isClassResourceTitle) ?: return null
        return quotedClassName(title)
    }

    /** The class name inside a quoted string such as `'apache::mod'`, or null if it is not one. */
    fun quotedClassName(element: PsiElement): String? {
        val text = element.text
        if (text.length < 3 || text.first() != text.last() || text.first() !in "'\"") return null
        return text.substring(1, text.lastIndex).takeIf { CLASS_NAME.matches(it) }
    }

    private val CLASS_NAME = Regex("(?:::)?[a-z][\\w]*(?:::[a-z][\\w]*)*")

    val CLASS_CALLS = setOf("include", "require", "contain")
}
