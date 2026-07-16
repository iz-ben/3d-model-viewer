import org.jetbrains.changelog.Changelog

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
    id("org.jetbrains.changelog") version "2.2.1"
}

group = "ke.co.coterie.plugins"
version = providers.gradleProperty("pluginVersion").get()

changelog {
    version = providers.gradleProperty("pluginVersion")
    groups.empty()
}

val renderedChangeNotes: String = changelog.run {
    renderItem(
        (getOrNull(version.get()) ?: getLatest())
            .withHeader(false)
            .withEmptySections(false),
        Changelog.OutputType.HTML,
    )
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2025.2.4")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:
        // implementation("com.google.code.gson:gson:2.10.1")

        // Bundled JSON support — needed for JSON PSI (com.intellij.json.psi) used to map
        // caret/selection in the glTF JSON to the corresponding materials.
        bundledPlugin("com.intellij.modules.json")

        composeUI()

    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
            // This plugin targets the old JCEF API,
            // compatibility is capped at 253 (2025.3) until
            // we migrate to the new API.
            untilBuild = "253.*"
        }

        changeNotes = renderedChangeNotes
    }

    // JetBrains Marketplace publishing configuration
    // Uncomment and configure when ready to publish
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            recommended()
        }
        failureLevel = listOf(
            org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.DEPRECATED_API_USAGES,
            org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
            org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
        )
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
