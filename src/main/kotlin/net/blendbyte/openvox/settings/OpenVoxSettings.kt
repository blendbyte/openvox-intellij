package net.blendbyte.openvox.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.util.xmlb.XmlSerializerUtil

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
        return candidates.firstNotNullOfOrNull {
            PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(it)?.absolutePath
        }
    }

    companion object {
        fun getInstance(): OpenVoxSettings = service()
    }
}
