package net.blendbyte.openvox

import com.intellij.openapi.extensions.PluginId
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.indexing.ID
import net.blendbyte.openvox.index.OpenVoxDeclarationIndex
import net.blendbyte.openvox.index.OpenVoxHieraIndex
import net.blendbyte.openvox.index.OpenVoxRubyFunctionIndex

class OpenVoxPluginIdentityTest : UsefulTestCase() {
    fun testIndicesCanLoadWhileLegacyIdsRemainRegistered() {
        val legacy = listOf("declarations", "hiera", "rubyFunctions").map {
            LegacyId("net.blendbyte.openvox.$it")
        }
        try {
            val current = listOf(OpenVoxDeclarationIndex().name, OpenVoxHieraIndex().name, OpenVoxRubyFunctionIndex().name)
            current.zip(legacy).forEach { (newId, oldId) ->
                assertNotSame(oldId, newId)
                assertTrue(newId.name.startsWith("net.blendbyte.openvox-intellij."))
                assertFalse(oldId.uniqueId == newId.uniqueId)
            }
        } finally {
            legacy.forEach(ID<*, *>::unloadId)
        }
    }

    private class LegacyId(name: String) : ID<String, Void>(name, PluginId.getId("net.blendbyte.openvox"))
}
