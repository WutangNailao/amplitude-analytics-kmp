@file:OptIn(KotlinNativeCacheApi::class)

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    android {
        namespace = "com.amplitude.kmp.sample.shared"
        compileSdk = libs.versions.sampleAndroidCompileSdk.get().toInt()
        minSdk = libs.versions.sampleAndroidMinSdk.get().toInt()

        androidResources {
            enable = true
        }

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "AmplitudeKMPSample"
            isStatic = true
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.configureEach {
            freeCompilerArgs += "-Xoverride-konan-properties=minVersion.ios=${libs.versions.sampleIosDeploymentTarget.get()}"
            disableNativeCache(
                version = DisableCacheInKotlinVersion.`2_4_21`,
                reason = "Precompiled ARM64 caches target iOS 15, above the sample's iOS 14.1 deployment target",
            )
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.core)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)

            // Consume published artifacts to exercise the SDK's Maven metadata.
            implementation("${project.group}:amplitude-analytics-kmp:${project.version}")
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation(libs.compose.ui.tooling.preview)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
