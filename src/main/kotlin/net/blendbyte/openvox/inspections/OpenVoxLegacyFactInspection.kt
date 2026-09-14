package net.blendbyte.openvox.inspections

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import net.blendbyte.openvox.resolve.OpenVoxElementFactory
import net.blendbyte.openvox.resolve.OpenVoxScope
import net.blendbyte.openvox.psi.OpenVoxTypes as T

class OpenVoxLegacyFactInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element.node?.elementType != T.VARIABLE_EXPR) return
                val name = OpenVoxScope.normalise(element.text)
                val replacement = OpenVoxInspectionSupport.LEGACY_FACTS[name] ?: return
                if (OpenVoxScope.resolve(element, name) != null) return

                holder.registerProblem(
                    element,
                    "Legacy fact '${element.text}'; use \$$replacement",
                    ProblemHighlightType.WEAK_WARNING,
                    UseStructuredFactFix(replacement),
                )
            }
        }
}

private class UseStructuredFactFix(private val replacement: String) : LocalQuickFix {

    override fun getFamilyName(): String = "Use the structured fact"

    override fun getName(): String = "Replace with \$$replacement"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement ?: return
        val file = OpenVoxElementFactory.createFile(project, "\$x = \$$replacement")
        val assignment = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
            .firstOrNull { it.node?.elementType == T.ASSIGN_EXPR } ?: return
        val value = assignment.lastChild ?: return
        element.replace(value)
    }
}
