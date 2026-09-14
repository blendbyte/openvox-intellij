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
        id = "net.blendbyte.openvox"
        name = "OpenVox and Puppet Language"
        version = project.version.toString()
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
