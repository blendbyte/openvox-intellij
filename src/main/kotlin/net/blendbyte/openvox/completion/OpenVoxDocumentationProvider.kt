package net.blendbyte.openvox.completion

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import net.blendbyte.openvox.index.OpenVoxDeclarationKind
import net.blendbyte.openvox.knowledge.OpenVoxKnowledge
import net.blendbyte.openvox.psi.OpenVoxNamedElement
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxDocumentationProvider : AbstractDocumentationProvider() {

    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null) return null

        (element as? OpenVoxNamedElement)?.let { declaration ->
            val kind = OpenVoxDeclarationKind.of(declaration)
            if (kind != null) return renderDeclaration(declaration, kind)
        }

        val word = (originalElement ?: element).text ?: return null
        return renderKnown(word, originalElement ?: element)
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        val declaration = element as? OpenVoxNamedElement ?: return null
        val kind = OpenVoxDeclarationKind.of(declaration) ?: return null
        return "${kind.presentableName} ${declaration.name}"
    }

    private fun renderDeclaration(declaration: OpenVoxNamedElement, kind: OpenVoxDeclarationKind): String {
        val builder = StringBuilder()
        builder.append(DEFINITION_START)
        builder.append("${kind.presentableName} <b>${declaration.name}</b>")

        val parameters = declaration.node?.findChildByType(T.PARAMETER_LIST)?.psi
        if (parameters != null) {
            builder.append("<br/>")
            PsiTreeUtil.findChildrenOfType(parameters, PsiElement::class.java)
                .filter { it.node?.elementType == T.PARAMETER }
                .forEach { parameter -> builder.append("&nbsp;&nbsp;${escape(parameter.text)}<br/>") }
        }
        builder.append(DEFINITION_END)

        commentAbove(declaration)?.let {
            builder.append(CONTENT_START).append(escape(it)).append(CONTENT_END)
        }
        return builder.toString()
    }

    private fun renderKnown(word: String, context: PsiElement): String? {
        OpenVoxKnowledge.functions[word]?.let { function ->
            val signatures = function.signatures.ifEmpty { listOf("") }
                .joinToString("<br/>") { "${function.name}(${escape(it)})" }
            val returns = if (function.returnType.isEmpty()) "" else " &rarr; ${escape(function.returnType)}"
            return DEFINITION_START + signatures + returns + DEFINITION_END +
                CONTENT_START + escape(function.doc) + CONTENT_END
        }
        OpenVoxKnowledge.dataTypes[word]?.let { type ->
            return DEFINITION_START + "data type <b>${type.name}</b>" + DEFINITION_END +
                CONTENT_START + escape(type.doc) + CONTENT_END
        }
        OpenVoxKnowledge.resourceTypes[word.lowercase()]?.let { type ->
            return DEFINITION_START + "resource type <b>${type.name}</b>" + DEFINITION_END +
                CONTENT_START + escape(type.doc) + CONTENT_END
        }
        val resource = generateSequence(context) { it.parent }
            .firstOrNull { it.node?.elementType == T.RESOURCE_EXPRESSION }
        val typeName = resource?.node?.findChildByType(T.RESOURCE_TYPE)?.text?.lowercase()
        if (typeName != null) {
            OpenVoxKnowledge.attribute(typeName, word)?.let { attribute ->
                val values = if (attribute.values.isEmpty()) ""
                else "<br/>Values: " + attribute.values.joinToString(", ") { "<code>$it</code>" }
                return DEFINITION_START + "${attribute.kind} <b>${attribute.name}</b> of $typeName" +
                    DEFINITION_END + CONTENT_START + escape(attribute.doc) + values + CONTENT_END
            }
        }
        return null
    }

    private fun commentAbove(declaration: PsiElement): String? {
        val lines = mutableListOf<String>()
        var sibling = declaration.prevSibling
        while (sibling != null) {
            when {
                sibling.node?.elementType == T.LINE_COMMENT ->
                    lines += sibling.text.removePrefix("#").trim()
                sibling is com.intellij.psi.PsiWhiteSpace && !sibling.text.contains("\n\n") -> Unit
                else -> break
            }
            sibling = sibling.prevSibling
        }
        return lines.asReversed().joinToString(" ").ifBlank { null }
    }

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private companion object {
        const val DEFINITION_START = "<div class='definition'><pre>"
        const val DEFINITION_END = "</pre></div>"
        const val CONTENT_START = "<div class='content'>"
        const val CONTENT_END = "</div>"
    }
}
