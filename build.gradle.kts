import org.jetbrains.changelog.Changelog

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("org.jetbrains.intellij.platform") version "2.18.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
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
        intellijIdea("2026.2")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:
        // implementation("com.google.code.gson:gson:2.10.1")

        // Bundled JSON support — needed for JSON PSI (com.intellij.json.psi) used to map
        // caret/selection in the glTF JSON to the corresponding materials.
        bundledPlugin("com.intellij.modules.json")

        // JCEF moved out of the platform core into its own bundled plugin in 2026.2;
        // it provides JBCefBrowser and the org.cef.* resource-handler API.
        bundledPlugin("com.intellij.modules.jcef")

        composeUI()

    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "262"
            // Targets the new JCEF resource-handler API (open/read/skip),
            // available from 2026.2 (build 262) onward.
            untilBuild = "262.*"
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
            // DEPRECATED_API_USAGES is intentionally NOT a failure: the 2026.2+
            // out-of-process JCEF ("cef_server") bridge invokes the deprecated
            // CefResourceHandler.processRequest/readResponse methods over Thrift,
            // so Model3DResourceHandler must override them to work at runtime.
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
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        // Emit real JVM default methods so Kotlin does not generate delegating
        // override bridges for Java interface default methods (e.g. ToolWindowFactory);
        // those bridges otherwise trip the plugin verifier's deprecated/experimental checks.
        freeCompilerArgs.add("-jvm-default=no-compatibility")
    }
}
