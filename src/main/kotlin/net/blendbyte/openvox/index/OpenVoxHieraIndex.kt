package net.blendbyte.openvox.index

import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor

class OpenVoxHieraIndex : ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void> = KEY
    override fun getVersion(): Int = 1
    override fun dependsOnFileContent(): Boolean = true
    override fun getKeyDescriptor() = EnumeratorStringDescriptor.INSTANCE
    override fun getInputFilter() = FileBasedIndex.InputFilter { it.extension == "yaml" || it.extension == "yml" }
    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { content ->
        KEYS.findAll(content.contentAsText).map { match ->
            match.groupValues.drop(1).first { it.isNotEmpty() }
        }.associateWith { null }
    }

    companion object {
        val KEY: ID<String, Void> = ID.create("net.blendbyte.openvox-intellij.hiera")
        private const val NAME = "[a-z][\\w]*(?:::[a-zA-Z_][\\w]*)+"
        private val KEYS = Regex("(?m)^[ \\t]*(?:($NAME)|\"($NAME)\"|'($NAME)'):[ \\t]*(?:[^\\r\\n]*)$")
    }
}
