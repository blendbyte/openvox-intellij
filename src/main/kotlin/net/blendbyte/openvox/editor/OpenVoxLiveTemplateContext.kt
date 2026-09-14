package net.blendbyte.openvox.editor

import com.intellij.codeInsight.template.TemplateActionContext
import com.intellij.codeInsight.template.TemplateContextType
import net.blendbyte.openvox.OpenVoxLanguage

class OpenVoxLiveTemplateContext : TemplateContextType("OpenVox") {
    override fun isInContext(context: TemplateActionContext): Boolean =
        context.file.language.isKindOf(OpenVoxLanguage)
}
