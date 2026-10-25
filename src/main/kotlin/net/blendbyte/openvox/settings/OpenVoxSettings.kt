package net.blendbyte.openvox.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.xmlb.XmlSerializerUtil
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

@Service(Service.Level.APP)
@State(name = "OpenVoxSettings", storages = [Storage("openvox.xml")])
class OpenVoxSettings : PersistentStateComponent<OpenVoxSettings> {

    var puppetExecutable: String = ""
    var puppetLintExecutable: String = ""
    var runParserValidate: Boolean = true
    var runPuppetLint: Boolean = true

    override fun getState(): OpenVoxSettings = this

    override fun loadState(state: OpenVoxSettings) = XmlSerializerUtil.copyBean(state, this)

    fun resolvedPuppet(): String? = resolve(puppetExecutable, "puppet", "openvox")

    fun resolvedPuppetLint(): String? = resolve(puppetLintExecutable, "puppet-lint")

    private fun resolve(configured: String, vararg candidates: String): String? {
        if (configured.isNotBlank()) return configured
        return candidates.firstNotNullOfOrNull(::findOnPath)
    }

    // PathEnvironmentVariableUtil.findFirst replaces the deprecated lookups only from 2026.3.
    internal fun findOnPath(baseName: String): String? {
        val directories = PathEnvironmentVariableUtil.getPathVariableValue()?.split(File.pathSeparatorChar).orEmpty()
        val names = if (SystemInfo.isWindows) {
            (System.getenv("PATHEXT") ?: ".COM;.EXE;.BAT;.CMD").split(';').filter(String::isNotBlank).map { baseName + it }
        } else listOf(baseName)
        return directories.asSequence()
            .filter(String::isNotBlank)
            .flatMap { directory -> names.asSequence().map { runCatching { Path.of(directory, it) }.getOrNull() } }
            .firstOrNull { it != null && Files.isRegularFile(it) && Files.isExecutable(it) }
            ?.toAbsolutePath()?.toString()
    }

    companion object {
        fun getInstance(): OpenVoxSettings = service()
    }
}
