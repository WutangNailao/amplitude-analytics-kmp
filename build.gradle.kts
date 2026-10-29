import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.cocoapods) apply false
    alias(libs.plugins.dokka)
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.ktlint) apply false
}

val ktlintEngineVersion = libs.versions.ktlintEngine.get()

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<KtlintExtension> {
        version.set(ktlintEngineVersion)
        filter {
            exclude { it.file.toPath().startsWith(layout.buildDirectory.get().asFile.toPath()) }
        }
    }

    group = project.findProperty("GROUP") ?: ""
    version = project.findProperty("VERSION_NAME") ?: "0.0.1-SNAPSHOT"
}

dependencies {
    dokka(project(":kmp"))
}
