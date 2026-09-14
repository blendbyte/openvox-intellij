package net.blendbyte.openvox.inspections

import com.intellij.psi.PsiElement
import net.blendbyte.openvox.index.OpenVoxDeclarationKind
import net.blendbyte.openvox.index.OpenVoxRubyFunctionIndex
import net.blendbyte.openvox.knowledge.OpenVoxKnowledge
import net.blendbyte.openvox.resolve.OpenVoxResolver
import net.blendbyte.openvox.psi.OpenVoxTypes as T

internal object OpenVoxInspectionSupport {

    val BUILT_IN_VARIABLES = setOf(
        "facts", "trusted", "server_facts", "environment", "title", "name",
        "module_name", "caller_module_name", "settings", "strict",
    )

    val LEGACY_FACTS = mapOf(
        "osfamily" to "facts['os']['family']",
        "operatingsystem" to "facts['os']['name']",
        "operatingsystemrelease" to "facts['os']['release']['full']",
        "operatingsystemmajrelease" to "facts['os']['release']['major']",
        "lsbdistcodename" to "facts['os']['distro']['codename']",
        "lsbdistid" to "facts['os']['distro']['id']",
        "lsbdistdescription" to "facts['os']['distro']['description']",
        "lsbdistrelease" to "facts['os']['distro']['release']['full']",
        "architecture" to "facts['os']['architecture']",
        "hardwaremodel" to "facts['os']['hardware']",
        "fqdn" to "facts['networking']['fqdn']",
        "hostname" to "facts['networking']['hostname']",
        "domain" to "facts['networking']['domain']",
        "ipaddress" to "facts['networking']['ip']",
        "ipaddress6" to "facts['networking']['ip6']",
        "netmask" to "facts['networking']['netmask']",
        "network" to "facts['networking']['network']",
        "macaddress" to "facts['networking']['mac']",
        "interfaces" to "facts['networking']['interfaces']",
        "memorysize" to "facts['memory']['system']['total']",
        "memoryfree" to "facts['memory']['system']['available']",
        "processorcount" to "facts['processors']['count']",
        "physicalprocessorcount" to "facts['processors']['physicalcount']",
        "id" to "facts['identity']['user']",
        "gid" to "facts['identity']['group']",
        "uptime" to "facts['system_uptime']['uptime']",
        "uptime_seconds" to "facts['system_uptime']['seconds']",
        "timezone" to "facts['timezone']",
        "is_virtual" to "facts['is_virtual']",
        "virtual" to "facts['virtual']",
        "kernel" to "facts['kernel']",
        "kernelrelease" to "facts['kernelrelease']",
        "kernelversion" to "facts['kernelversion']",
        "kernelmajversion" to "facts['kernelmajversion']",
        "path" to "facts['path']",
        "clientcert" to "trusted['certname']",
    )

    fun functionExists(element: PsiElement, name: String): Boolean {
        val bare = name.removePrefix("::")
        if (OpenVoxKnowledge.functions.containsKey(bare)) return true
        if (bare in STATEMENT_CALLS) return true
        val project = element.project
        if (OpenVoxResolver.find(project, OpenVoxDeclarationKind.FUNCTION, bare).isNotEmpty()) return true
        return OpenVoxRubyFunctionIndex.exists(project, bare)
    }

    private val STATEMENT_CALLS = setOf(
        "require", "realize", "include", "contain", "tag",
        "debug", "info", "notice", "warning", "err",
        "fail", "break", "next", "return",
    )

    fun attributeNamesFor(element: PsiElement, typeName: String): Set<String>? {
        val bare = typeName.removePrefix("::").lowercase()
        OpenVoxKnowledge.resourceTypes[bare]?.let { type ->
            return (type.attributes.map { it.name } + OpenVoxKnowledge.metaparameters.map { it.name }).toSet()
        }
        val define = OpenVoxResolver.find(element.project, OpenVoxDeclarationKind.DEFINE, bare).firstOrNull()
            ?: OpenVoxResolver.find(element.project, OpenVoxDeclarationKind.CLASS, bare).firstOrNull()
            ?: return null
        val parameters = define.node?.findChildByType(T.PARAMETER_LIST) ?: return null
        val names = parameters.getChildren(null)
            .filter { it.elementType == T.PARAMETER }
            .mapNotNull { it.findChildByType(T.VARIABLE)?.text?.removePrefix("$") }
        return (names + OpenVoxKnowledge.metaparameters.map { it.name }).toSet()
    }
}
