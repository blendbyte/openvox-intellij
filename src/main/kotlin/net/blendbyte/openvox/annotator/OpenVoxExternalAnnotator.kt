package net.blendbyte.openvox.annotator

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import net.blendbyte.openvox.OpenVoxLanguage
import net.blendbyte.openvox.settings.OpenVoxSettings
import com.intellij.openapi.util.io.FileUtil
import java.nio.file.Files
import java.nio.file.Path

data class OpenVoxExternalMessage(
    val line: Int,
    val column: Int,
    val severity: HighlightSeverity,
    val message: String,
    val source: String,
)

data class OpenVoxAnnotatorInput(val text: String, val path: Path?)

/**
 * Where the unsaved copy is written and where puppet-lint runs. The copy keeps the file's path
 * below its module root, so puppet-lint's autoloader_layout check sees the real layout, and
 * puppet-lint starts in the nearest directory with a .puppet-lint.rc so project settings apply.
 */
internal data class OpenVoxLintLayout(val relativePath: String, val workDirectory: Path?) {
    companion object {
        fun of(path: Path?): OpenVoxLintLayout {
            val parent = path?.parent ?: return OpenVoxLintLayout("manifest.pp", null)
            val manifests = generateSequence(parent) { it.parent }.firstOrNull { it.fileName?.toString() == "manifests" }
            val moduleRoot = manifests?.parent ?: parent
            val relative = moduleRoot.relativize(path).joinToString("/")
            val workDirectory = generateSequence(moduleRoot) { it.parent }
                .firstOrNull { Files.isRegularFile(it.resolve(".puppet-lint.rc")) } ?: moduleRoot
            return OpenVoxLintLayout(relative, workDirectory)
        }
    }
}

class OpenVoxExternalAnnotator :
    ExternalAnnotator<OpenVoxAnnotatorInput, List<OpenVoxExternalMessage>>() {

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): OpenVoxAnnotatorInput? {
        // Registered for OpenVox, which also reaches the EPP dialect; templates are not manifests.
        if (hasErrors || file.language != OpenVoxLanguage) return null
        val settings = OpenVoxSettings.getInstance()
        if (!settings.runParserValidate && !settings.runPuppetLint) return null
        val path = file.virtualFile?.let { it.fileSystem.getNioPath(it) }
        return OpenVoxAnnotatorInput(editor.document.text, path)
    }

    override fun doAnnotate(collected: OpenVoxAnnotatorInput?): List<OpenVoxExternalMessage> {
        val input = collected ?: return emptyList()
        val settings = OpenVoxSettings.getInstance()

        val puppet = if (settings.runParserValidate) settings.resolvedPuppet() else null
        val lint = if (settings.runPuppetLint) settings.resolvedPuppetLint() else null
        if (puppet == null && lint == null) return emptyList()
        val layout = OpenVoxLintLayout.of(input.path)
        var directory: Path? = null
        return try {
            directory = Files.createTempDirectory("openvox-")
            val copy = directory.resolve(layout.relativePath)
            Files.createDirectories(copy.parent)
            Files.writeString(copy, input.text)
            buildList {
                puppet?.let { addAll(runParserValidate(it, copy.toString())) }
                lint?.let { addAll(runPuppetLint(it, copy.toString(), layout.workDirectory)) }
            }
        } catch (exception: Exception) {
            if (exception is ControlFlowException) throw exception
            emptyList()
        } finally {
            directory?.let { runCatching { FileUtil.delete(it) } }
        }
    }

    override fun apply(file: PsiFile, messages: List<OpenVoxExternalMessage>?, holder: AnnotationHolder) {
        val document = file.viewProvider.document ?: return
        messages.orEmpty().forEach { message ->
            val range = rangeFor(document, message) ?: return@forEach
            holder.newAnnotation(message.severity, "${message.source}: ${message.message}")
                .range(range)
                .create()
        }
    }

    private fun rangeFor(document: Document, message: OpenVoxExternalMessage): TextRange? {
        val line = (message.line - 1).coerceIn(0, (document.lineCount - 1).coerceAtLeast(0))
        val lineStart = document.getLineStartOffset(line)
        val lineEnd = document.getLineEndOffset(line)
        val start = (lineStart + (message.column - 1).coerceAtLeast(0)).coerceIn(lineStart, lineEnd)
        return if (start >= lineEnd) TextRange(lineStart, lineEnd) else TextRange(start, lineEnd)
    }

    private fun runParserValidate(executable: String, path: String): List<OpenVoxExternalMessage> {
        val output = run(GeneralCommandLine(executable, "parser", "validate", path)) ?: return emptyList()
        return VALIDATE_PATTERN.findAll(output).map { match ->
            OpenVoxExternalMessage(
                line = match.groupValues[2].toIntOrNull() ?: 1,
                column = match.groupValues[3].toIntOrNull() ?: 1,
                severity = HighlightSeverity.ERROR,
                message = match.groupValues[1].trim(),
                source = "puppet",
            )
        }.toList()
    }

    private fun runPuppetLint(executable: String, path: String, workDirectory: Path?): List<OpenVoxExternalMessage> {
        val command = GeneralCommandLine(
            executable,
            "--relative",
            "--log-format", "%{line}:%{column}:%{kind}:%{message}",
            path,
        ).withWorkDirectory(workDirectory?.toFile())
        val output = run(command) ?: return emptyList()
        return output.lineSequence().mapNotNull { line ->
            val parts = line.split(':', limit = 4)
            if (parts.size < 4) return@mapNotNull null
            OpenVoxExternalMessage(
                line = parts[0].trim().toIntOrNull() ?: return@mapNotNull null,
                column = parts[1].trim().toIntOrNull() ?: 1,
                severity = if (parts[2].trim().equals("error", ignoreCase = true)) HighlightSeverity.ERROR
                else HighlightSeverity.WARNING,
                message = parts[3].trim(),
                source = "puppet-lint",
            )
        }.toList()
    }

    private fun run(command: GeneralCommandLine): String? = try {
        val result = ExecUtil.execAndGetOutput(command.withRedirectErrorStream(true), TIMEOUT_MS)
        result.stdout
    } catch (exception: Exception) {
        if (exception is ControlFlowException) throw exception
        null
    }

    private companion object {
        const val TIMEOUT_MS = 10_000

        val VALIDATE_PATTERN = Regex("""Error:\s*(.+?)\s*\(file:[^,]+,\s*line:\s*(\d+),\s*column:\s*(\d+)\)""")
    }
}
