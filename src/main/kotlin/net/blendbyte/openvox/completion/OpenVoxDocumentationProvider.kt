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

        commentAbove(declaration)?.let { builder.append(renderComment(it)) }
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

    /** The `#` comment lines directly above [declaration], keeping indentation after the `# `. */
    private fun commentAbove(declaration: PsiElement): List<String>? {
        val lines = mutableListOf<String>()
        var sibling = declaration.prevSibling
        while (sibling != null) {
            when {
                sibling.node?.elementType == T.LINE_COMMENT ->
                    lines += sibling.text.removePrefix("#").removePrefix(" ").trimEnd()
                sibling is com.intellij.psi.PsiWhiteSpace && !sibling.text.contains("\n\n") -> Unit
                else -> break
            }
            sibling = sibling.prevSibling
        }
        return lines.asReversed().takeIf { it.any(String::isNotBlank) }
    }

    private class Tag(val name: String?, val argument: String, val body: MutableList<String> = mutableListOf())

    /** Renders puppet-strings comments: description, @summary, @param, @example and other tags. */
    private fun renderComment(lines: List<String>): String {
        val description = Tag(null, "")
        val tags = mutableListOf(description)
        var current = description
        for (line in lines) {
            val match = TAG.matchEntire(line.trim())
            if (match != null) {
                current = Tag(match.groupValues[1], match.groupValues[2]).also { tags += it }
                continue
            }
            // A blank line ends a tag, except inside an example, which ends at the first unindented line.
            val inExample = current.name == "example"
            if (inExample && line.isNotBlank() && !line.first().isWhitespace() || !inExample && line.isBlank()) {
                current = description
            }
            current.body += line
        }
        fun prose(tag: Tag) = escape((listOf(tag.argument) + tag.body).joinToString(" ") { it.trim() }.trim())

        val content = StringBuilder()
        tags.filter { it.name == "summary" }.forEach { content.append("<p>").append(prose(it)).append("</p>") }
        description.body.joinToString("\n").trim().split(Regex("\n\\s*\n")).filter { it.isNotBlank() }
            .forEach { content.append("<p>").append(escape(it.replace('\n', ' '))).append("</p>") }
        tags.filter { it.name == "example" }.forEach { example ->
            if (example.argument.isNotBlank()) content.append("<p><i>").append(escape(example.argument)).append("</i></p>")
            content.append("<pre><code>").append(escape(example.body.joinToString("\n").trimIndent().trim('\n'))).append("</code></pre>")
        }

        val sections = StringBuilder()
        val parameters = tags.filter { it.name == "param" }
        if (parameters.isNotEmpty()) {
            val rows = parameters.joinToString("<br/>") { parameter ->
                val name = parameter.argument.substringBefore(' ')
                val rest = Tag(null, parameter.argument.substringAfter(' ', ""), parameter.body)
                "<code>${escape(name)}</code> &ndash; ${prose(rest)}"
            }
            sections.append(section("Parameters:", rows))
        }
        tags.filter { it.name != null && it.name !in setOf("summary", "param", "example") }.forEach {
            sections.append(section("${it.name!!.replaceFirstChar(Char::uppercaseChar)}:", prose(it)))
        }

        return (if (content.isEmpty()) "" else CONTENT_START + content + CONTENT_END) +
            (if (sections.isEmpty()) "" else SECTIONS_START + sections + SECTIONS_END)
    }

    private fun section(header: String, body: String) =
        "<tr><td valign='top' class='section'><p>$header</td><td valign='top'>$body</td></tr>"

    private fun escape(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private companion object {
        const val DEFINITION_START = "<div class='definition'><pre>"
        const val DEFINITION_END = "</pre></div>"
        const val CONTENT_START = "<div class='content'>"
        const val CONTENT_END = "</div>"
        const val SECTIONS_START = "<table class='sections'>"
        const val SECTIONS_END = "</table>"
        val TAG = Regex("@(\\w+)\\s*(.*)")
    }
}
