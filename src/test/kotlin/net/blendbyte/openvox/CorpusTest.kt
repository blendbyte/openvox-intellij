package net.blendbyte.openvox

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import net.blendbyte.openvox.parser.EppParserDefinition
import net.blendbyte.openvox.parser.OpenVoxParserDefinition
import java.io.File

class CorpusTest : CorpusParsingTest("pp")
class EppCorpusTest : CorpusParsingTest("epp")

abstract class CorpusParsingTest(private val extension: String) :
    ParsingTestCase(
        "", extension,
        if (extension == "epp") EppParserDefinition() else OpenVoxParserDefinition(),
        if (extension == "epp") OpenVoxParserDefinition() else EppParserDefinition(),
    ) {
    override fun getTestDataPath() = "src/test/testData"

    fun testCorpusParses() {
        val paths = requireNotNull(System.getProperty("openvox.corpus")) { "Set -Dopenvox.corpus to run external corpus tests" }
        val roots = paths.split(File.pathSeparatorChar).filter(String::isNotBlank)
        val files = roots.flatMap { root -> File(root).walkTopDown().filter { it.isFile && it.extension == extension }.toList() }
        assertTrue("No .$extension files in corpus: $roots", files.isNotEmpty())
        val failures = files.mapNotNull { file ->
            val text = file.readText()
            val psi = createFile(file.name, text)
            ensureParsed(psi)
            val error = PsiTreeUtil.findChildOfType(psi, PsiErrorElement::class.java) ?: return@mapNotNull null
            val line = text.take(error.textOffset).count { it == '\n' } + 1
            "${file.path}:$line ${error.errorDescription}"
        }
        println("CORPUS .$extension: ${files.size} files, ${failures.size} failures")
        failures.take(40).forEach(::println)
        val allowed = System.getProperty("openvox.corpus.maxFailures")?.toIntOrNull() ?: 0
        assertTrue("${failures.size}/${files.size} .$extension files failed (allowed: $allowed)", failures.size <= allowed)
    }
}
