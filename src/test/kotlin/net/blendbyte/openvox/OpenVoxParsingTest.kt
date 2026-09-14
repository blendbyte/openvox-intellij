package net.blendbyte.openvox

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import net.blendbyte.openvox.parser.EppParserDefinition
import net.blendbyte.openvox.parser.OpenVoxParserDefinition

class OpenVoxParsingTest : ParsingTestCase("", "pp", OpenVoxParserDefinition(), EppParserDefinition()) {

    override fun getTestDataPath() = "src/test/testData"
    override fun skipSpaces() = false
    override fun includeRanges() = true

    private fun assertParses(name: String, code: String) {
        val file = createPsiFile(name, code)
        ensureParsed(file)
        val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
        if (errors.isNotEmpty()) {
            val details = errors.joinToString("\n") { e ->
                val line = code.substring(0, e.textOffset).count { it == '\n' } + 1
                "  line $line: ${e.errorDescription} at '${e.text.take(40)}'"
            }
            fail("$name did not parse cleanly:\n$details\n--- tree ---\n${toParseTreeText(file, true, false)}")
        }
    }

    fun testClassWithModernDataTypes() = assertParses(
        "types", """
        class profile::agent (
          Enum['running','stopped'] ${'$'}ensure = 'running',
          Optional[Sensitive[String[1]]] ${'$'}token = undef,
          Variant[Boolean, Enum['auto']] ${'$'}mode = 'auto',
          Hash[String[1], Struct[{ 'port' => Integer[1,65535], Optional['tls'] => Boolean }]] ${'$'}servers = {},
          Array[Init[Integer, 8]] ${'$'}octals = [],
          Timespan ${'$'}grace = Timespan(3600),
          Pattern[/\A[a-z]+\z/] ${'$'}name = 'x',
        ) {
        }
        """.trimIndent()
    )

    fun testTypeAliasAndFunction() = assertParses(
        "alias", """
        type Profile::Port = Integer[1, 65535]
        type Profile::Endpoint = Struct[{ host => String[1], port => Profile::Port }]

        function profile::render(String[1] ${'$'}tpl, Hash ${'$'}vars) >> String {
          epp(${'$'}tpl, ${'$'}vars)
        }
        """.trimIndent()
    )

    fun testResourcesAndRelationships() = assertParses(
        "resources", """
        class ntp {
          package { 'ntp': ensure => installed }

          file { '/etc/ntp.conf':
            ensure  => file,
            content => template('ntp/ntp.conf.erb'),
            require => Package['ntp'],
          }

          @@nagios_service { "check_ntp_${'$'}{facts['networking']['fqdn']}":
            check_command => 'check_ntp',
          }

          Package['ntp'] -> File['/etc/ntp.conf'] ~> Service['ntp']

          Service <| tag == 'ntp' |>
        }
        """.trimIndent()
    )

    fun testControlFlowAndLambdas() = assertParses(
        "control", """
        class demo(Array[String] ${'$'}items = []) {
          ${'$'}items.each |${'$'}index, ${'$'}item| {
            notify { "item-${'$'}{index}": message => ${'$'}item }
          }

          ${'$'}kind = ${'$'}facts['os']['family'] ? {
            'RedHat' => 'yum',
            'Debian' => 'apt',
            default  => 'unknown',
          }

          case ${'$'}kind {
            'yum', 'apt': { include profile::pkg }
            /^un/:        { fail('unsupported') }
            default:      { }
          }

          unless ${'$'}items =~ Array[String[1]] {
            warning('empty')
          }
        }
        """.trimIndent()
    )

    fun testHeredoc() = assertParses(
        "heredoc", """
        class h {
          ${'$'}text = @(END)
            plain heredoc body
            second line
            | END

          notify { 'x': message => ${'$'}text }
        }
        """.trimIndent()
    )

}
