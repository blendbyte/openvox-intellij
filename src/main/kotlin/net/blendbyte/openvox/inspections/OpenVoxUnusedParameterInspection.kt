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

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.node?.elementType != T.PARAMETER) return
                val variable = element.node?.findChildByType(T.VARIABLE)?.psi ?: return

                val owner = generateSequence(element) { it.parent }
                    .firstOrNull { it.node?.elementType in LOCAL_SCOPES } ?: return
                val name = OpenVoxScope.normalise(variable.text)

                val used = PsiTreeUtil.findChildrenOfType(owner, PsiElement::class.java).any { candidate ->
                    (candidate.node?.elementType == T.VARIABLE_EXPR || OpenVoxReferenceContext.isInterpolationVariable(candidate)) &&
                        OpenVoxScope.normalise(candidate.text) == name && OpenVoxScope.resolve(candidate, name) === element
                }
                if (used) return

                holder.registerProblem(
                    variable,
                    "Parameter '${variable.text}' is never used",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                )
            }
        }
}
