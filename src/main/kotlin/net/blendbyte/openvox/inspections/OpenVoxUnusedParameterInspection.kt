package net.blendbyte.openvox.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import net.blendbyte.openvox.resolve.OpenVoxScope
import net.blendbyte.openvox.resolve.OpenVoxReferenceContext
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxUnusedParameterInspection : LocalInspectionTool() {

    private val LOCAL_SCOPES = setOf(T.DEFINE_DEFINITION, T.FUNCTION_DEFINITION, T.LAMBDA, T.PLAN_DEFINITION)
    private val ERB_FUNCTIONS = setOf("template", "inline_template")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.node?.elementType != T.PARAMETER) return
                val variable = element.node?.findChildByType(T.VARIABLE)?.psi ?: return

                val owner = generateSequence(element) { it.parent }
                    .firstOrNull { it.node?.elementType in LOCAL_SCOPES } ?: return
                val name = OpenVoxScope.normalise(variable.text)
                if (name.startsWith("_")) return

                val descendants = PsiTreeUtil.findChildrenOfType(owner, PsiElement::class.java)
                val used = descendants.any { candidate ->
                    (candidate.node?.elementType == T.VARIABLE_EXPR || OpenVoxReferenceContext.isInterpolationVariable(candidate)) &&
                        OpenVoxScope.normalise(candidate.text) == name && OpenVoxScope.resolve(candidate, name) === element
                }
                if (used) return
                if (descendants.any(::rendersErb)) return
                if (usedInHeredoc(owner, name)) return

                holder.registerProblem(
                    variable,
                    "Parameter '${variable.text}' is never used",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                )
            }
        }

    /** ERB templates read every variable in the calling scope, so their use cannot be seen here. */
    private fun rendersErb(element: PsiElement): Boolean {
        val type = element.node?.elementType
        if (type != T.FUNCTION_CALL_EXPR && type != T.BARE_FUNCTION_CALL) return false
        return element.firstChild?.text in ERB_FUNCTIONS
    }

    /** Heredoc bodies are not parsed, so look for an interpolation of the name in their text. */
    private fun usedInHeredoc(owner: PsiElement, name: String): Boolean {
        val pattern = Regex("""\$\{?(::)?${Regex.escape(name)}(?![A-Za-z0-9_])""")
        return PsiTreeUtil.collectElements(owner) { it.node?.elementType == T.HD_TEXT }
            .any { pattern.containsMatchIn(it.text) }
    }
}
