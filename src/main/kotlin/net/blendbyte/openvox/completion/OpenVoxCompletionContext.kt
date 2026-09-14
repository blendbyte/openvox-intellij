package net.blendbyte.openvox.completion

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import net.blendbyte.openvox.psi.OpenVoxTypes as T

sealed interface OpenVoxCompletionPlace {
    data class AttributeName(val resourceType: String) : OpenVoxCompletionPlace

    data class AttributeValue(val resourceType: String, val attributeName: String) : OpenVoxCompletionPlace

    data object DataType : OpenVoxCompletionPlace

    data object ResourceType : OpenVoxCompletionPlace

    data class FactName(val prefix: String, val trusted: Boolean) : OpenVoxCompletionPlace

    data object Statement : OpenVoxCompletionPlace
}

object OpenVoxCompletionContext {

    private val OPENING = setOf(T.LBRACE, T.SELBRACE, T.LBRACK, T.LISTSTART, T.LPAREN, T.WSLPAREN)
    private val CLOSING = setOf(T.RBRACE, T.RBRACK, T.RPAREN)
    private val ARROWS = setOf(T.FARROW, T.PARROW)
    private val SEPARATORS = setOf(T.COMMA, T.COLON, T.SEMIC)
    private val DEFINITION_KEYWORDS = setOf(T.CLASS, T.DEFINE, T.FUNCTION, T.PLAN)

    fun placeAt(position: PsiElement): OpenVoxCompletionPlace {
        val leaves = previousMeaningfulLeaves(position).toList()

        factKey(leaves)?.let { return it }

        var depth = 0
        var decided: OpenVoxCompletionPlace? = null
        var attributeName: String? = null

        for ((index, leaf) in leaves.withIndex()) {
            val type = leaf.node?.elementType ?: continue

            if (type in CLOSING) {
                depth++
                continue
            }
            if (type in OPENING) {
                if (depth > 0) {
                    depth--
                    continue
                }
                val owner = leaves.getOrNull(index + 1)
                val ownerType = owner?.node?.elementType
                val beforeOwner = leaves.getOrNull(index + 2)?.node?.elementType

                if (type == T.LBRACE &&
                    (ownerType == T.IDENTIFIER || ownerType == T.BARE_WORD || ownerType == T.CLASSREF)
                ) {
                    val resourceType = owner!!.text.removePrefix("::").lowercase()
                    return when (decided) {
                        is OpenVoxCompletionPlace.AttributeValue ->
                            OpenVoxCompletionPlace.AttributeValue(resourceType, attributeName.orEmpty())
                        null -> OpenVoxCompletionPlace.Statement
                        else -> OpenVoxCompletionPlace.AttributeName(resourceType)
                    }
                }

                if ((type == T.LPAREN || type == T.WSLPAREN) && beforeOwner in DEFINITION_KEYWORDS) {
                    return OpenVoxCompletionPlace.DataType
                }

                if ((type == T.LBRACK || type == T.LISTSTART) && ownerType == T.CLASSREF) {
                    return OpenVoxCompletionPlace.DataType
                }
                break
            }
            if (depth > 0) continue

            if (decided == null && type in ARROWS) {
                attributeName = leaves.getOrNull(index + 1)?.text
                decided = OpenVoxCompletionPlace.AttributeValue("", attributeName.orEmpty())
                continue
            }
            if (decided == null && type in SEPARATORS) {
                decided = OpenVoxCompletionPlace.AttributeName("")
                continue
            }
        }

        return when {
            inside(position, T.DATA_TYPE) || inside(position, T.DATA_TYPE_PARAMETERS) ||
                inside(position, T.TYPE_ALIAS) || inside(position, T.PARAMETER_LIST) ||
                position.text.firstOrNull()?.isUpperCase() == true -> OpenVoxCompletionPlace.DataType

            inside(position, T.RESOURCE_TYPE) -> OpenVoxCompletionPlace.ResourceType

            else -> OpenVoxCompletionPlace.Statement
        }
    }

    private fun factKey(leaves: List<PsiElement>): OpenVoxCompletionPlace? {
        val bracket = leaves.indexOfFirst { it.node?.elementType == T.LBRACK }
        if (bracket < 0 || bracket > 1) return null
        var index = bracket + 1
        val path = mutableListOf<String>()
        while (leaves.getOrNull(index)?.node?.elementType == T.RBRACK) {
            when (leaves.getOrNull(++index)?.node?.elementType) {
                T.SQ_STRING -> path += leaves[index++].text.drop(1).dropLast(1)
                T.DQ_END -> {
                    if (leaves.getOrNull(index + 1)?.node?.elementType != T.DQ_TEXT ||
                        leaves.getOrNull(index + 2)?.node?.elementType != T.DQ_START) return null
                    path += leaves[index + 1].text
                    index += 3
                }
                else -> return null
            }
            if (leaves.getOrNull(index++)?.node?.elementType != T.LBRACK) return null
        }
        val subject = leaves.getOrNull(index)?.text?.removePrefix("$")?.removePrefix("::")
        if (subject != "facts" && subject != "trusted") return null
        val prefix = if (path.isEmpty()) "" else path.asReversed().joinToString(".") + "."
        return OpenVoxCompletionPlace.FactName(prefix, subject == "trusted")
    }

    private fun previousMeaningfulLeaves(position: PsiElement): Sequence<PsiElement> =
        generateSequence(PsiTreeUtil.prevLeaf(position, true)) { PsiTreeUtil.prevLeaf(it, true) }
            .filter { it !is PsiWhiteSpace && it !is PsiComment && it.textLength > 0 }
            .take(400)

    private fun inside(element: PsiElement, type: com.intellij.psi.tree.IElementType): Boolean =
        generateSequence(element) { it.parent }.any { it.node?.elementType == type }
}
