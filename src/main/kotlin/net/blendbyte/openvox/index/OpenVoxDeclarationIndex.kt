package net.blendbyte.openvox.index

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import net.blendbyte.openvox.OpenVoxFileType
import net.blendbyte.openvox.psi.OpenVoxNamedElement
import net.blendbyte.openvox.psi.OpenVoxTypes as T

enum class OpenVoxDeclarationKind(val prefix: String, val presentableName: String) {
    CLASS("class", "class"),
    DEFINE("define", "defined type"),
    FUNCTION("function", "function"),
    TYPE_ALIAS("type", "type alias"),
    PLAN("plan", "plan"),
    NODE("node", "node"),
    ;

    companion object {
        fun of(element: PsiElement): OpenVoxDeclarationKind? = when (element.node?.elementType) {
            T.CLASS_DEFINITION -> CLASS
            T.DEFINE_DEFINITION -> DEFINE
            T.FUNCTION_DEFINITION -> FUNCTION
            T.TYPE_ALIAS -> TYPE_ALIAS
            T.PLAN_DEFINITION -> PLAN
            T.NODE_DEFINITION -> NODE
            else -> null
        }
    }
}

class OpenVoxDeclarationIndex : ScalarIndexExtension<String>() {

    override fun getName(): ID<String, Void> = KEY

    override fun getVersion(): Int = 2

    override fun dependsOnFileContent(): Boolean = true

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        FileBasedIndex.InputFilter { it.fileType == OpenVoxFileType }

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { content ->
        val file = content.psiFile
        declarationsIn(file).mapNotNull { declaration ->
            val kind = OpenVoxDeclarationKind.of(declaration) ?: return@mapNotNull null
            val name = declaration.name ?: return@mapNotNull null
            keyFor(kind, name)
        }.associateWith { null }
    }

    companion object {
        val KEY: ID<String, Void> = ID.create("net.blendbyte.openvox-intellij.declarations")

        fun keyFor(kind: OpenVoxDeclarationKind, name: String): String =
            "${kind.prefix}:${name.removePrefix("::").lowercase()}"

        fun declarationsIn(file: PsiFile): List<OpenVoxNamedElement> =
            CachedValuesManager.getCachedValue(file) {
                CachedValueProvider.Result.create(
                    PsiTreeUtil.findChildrenOfType(file, OpenVoxNamedElement::class.java)
                        .filter { OpenVoxDeclarationKind.of(it) != null },
                    file,
                )
            }
    }
}
