import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

val platformVersion: String by project

repositories {
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    intellijPlatform {
        intellijIdeaCommunity(platformVersion)
        testFramework(TestFrameworkType.Platform)
    }
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "net.blendbyte.openvox-intellij"
        name = "OpenVox Language"
        version = project.version.toString()
        changeNotes = """
            <h3>0.3.0</h3>
            <ul>
              <li>EPP templates are checked with <code>puppet epp validate</code>.</li>
              <li>puppet-lint uses your project's <code>.puppet-lint.rc</code> and no longer reports a false autoload layout error.</li>
              <li><code>class { 'name': }</code> resources navigate to the class, and their attributes are completed and checked against its parameters.</li>
              <li>Completion offers defined type and class parameters, resource types and keywords.</li>
              <li>Quick documentation renders puppet-strings comments with parameters and examples.</li>
              <li>The structure view shows icons and resource titles and includes resources in conditionals.</li>
              <li>Template and file references resolve in module repositories such as <code>puppetlabs-apache</code>.</li>
              <li>Hiera gutter links also work with <code>hieradata/</code> directories.</li>
              <li>Fewer false warnings for unused parameters, unresolved variables and unknown attributes.</li>
              <li>Fixes for EPP completion, heredoc tags, the formatter and legacy fact quick fixes.</li>
            </ul>
        """.trimIndent()
        vendor {
            name = "Blendbyte GmbH"
            url = "https://github.com/blendbyte/openvox-intellij"
        }
        ideaVersion {
            sinceBuild = "252"
            untilBuild = provider { null }
        }
    }
    pluginVerification {
        ides {
            recommended()
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }
}

val generatedSrc = layout.buildDirectory.dir("generated/sources/grammar")

val generateOpenVoxParser by tasks.registering(GenerateParserTask::class) {
    sourceFile = file("src/main/grammar/OpenVox.bnf")
    targetRootOutputDir = generatedSrc.map { it.asFile }
    pathToParser = "net/blendbyte/openvox/parser/OpenVoxParser.java"
    pathToPsiRoot = "net/blendbyte/openvox/psi"
    purgeOldFiles = true
}

val generateOpenVoxLexer by tasks.registering(GenerateLexerTask::class) {
    sourceFile = file("src/main/grammar/OpenVox.flex")
    targetOutputDir = generatedSrc.map { it.dir("net/blendbyte/openvox/lexer").asFile }
    skeleton = file("src/main/grammar/idea-flex.skeleton")
    purgeOldFiles = true
    dependsOn(generateOpenVoxParser)
}

sourceSets["main"].java.srcDir(generatedSrc)

tasks {
    named("build") {
        dependsOn("buildPlugin")
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn(generateOpenVoxLexer, generateOpenVoxParser)
    }
    withType<JavaCompile> {
        dependsOn(generateOpenVoxLexer, generateOpenVoxParser)
    }
}

tasks.test {
    if (System.getProperty("openvox.corpus").isNullOrBlank()) {
        exclude("**/CorpusTest.class", "**/EppCorpusTest.class")
    }
    for (key in listOf("openvox.corpus", "openvox.corpus.maxFailures")) {
        System.getProperty(key)?.let { systemProperty(key, it) }
    }
    testLogging { showStandardStreams = true }
}
