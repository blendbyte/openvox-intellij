package net.blendbyte.openvox.index

import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor

class OpenVoxRubyFunctionIndex : ScalarIndexExtension<String>() {

    override fun getName(): ID<String, Void> = KEY

    override fun getVersion(): Int = 1

    override fun dependsOnFileContent(): Boolean = true

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { file ->
        file.extension == "rb" && ("/puppet/functions/" in file.path || "/puppet/parser/functions/" in file.path)
    }

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { content ->
        val text = content.contentAsText
        val names = mutableSetOf<String>()
        MODERN.findAll(text).forEach { names += it.groupValues[1].lowercase() }
        LEGACY.findAll(text).forEach { names += it.groupValues[1].lowercase() }
        names.associateWith { null }
    }

    companion object {
        val KEY: ID<String, Void> = ID.create("net.blendbyte.openvox.rubyFunctions")

        private val MODERN = Regex("""create_function\(\s*:'?([\w:]+)'?""")
        private val LEGACY = Regex("""newfunction\(\s*:(\w+)""")

        fun exists(project: com.intellij.openapi.project.Project, name: String): Boolean =
            FileBasedIndex.getInstance().getContainingFiles(
                KEY,
                name.removePrefix("::").lowercase(),
                com.intellij.psi.search.GlobalSearchScope.allScope(project),
            ).isNotEmpty()
    }
}
