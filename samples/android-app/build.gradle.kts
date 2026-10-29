import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.composeCompiler)
}

val localProps = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.isFile) {
    localPropertiesFile.inputStream().use(localProps::load)
}
val amplitudeApiKey =
    localProps.getProperty("AMPLITUDE_API_KEY", "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

android {
    namespace = "com.amplitude.kmp.sample"
    compileSdk = libs.versions.sampleAndroidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.amplitude.kmp.sample"
        minSdk = libs.versions.sampleAndroidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "AMPLITUDE_API_KEY", "\"$amplitudeApiKey\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    }
}

// AGP 9 supplies Kotlin support; do not apply org.jetbrains.kotlin.android.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
    }
}

dependencies {
    implementation(project(":samples:kotlin-kmp-app"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    debugImplementation(libs.compose.ui.tooling)
}
