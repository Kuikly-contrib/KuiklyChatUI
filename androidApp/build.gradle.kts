plugins {
    id("com.android.application")
    kotlin("android")
}

fun localKimiSetting(name: String): String = System.getenv(name)
    ?: project.rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.readLines()
        ?.firstOrNull { it.startsWith("$name=") }
        ?.substringAfter('=')
        ?.trim()
        .orEmpty()

val kimiApiKey = localKimiSetting("KIMI_API_KEY")
// kimi-k2.6 with thinking disabled starts streaming A2UI in ~1s.
val kimiModel = localKimiSetting("KIMI_MODEL").ifEmpty { "kimi-k2.6" }

fun String.asBuildConfigString(): String = "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.kuikly.kuiklychat"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.kuikly.kuiklychat"
        minSdk = 23
        targetSdk = 30
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "KIMI_API_KEY", kimiApiKey.asBuildConfigString())
        buildConfigField("String", "KIMI_MODEL", kimiModel.asBuildConfigString())
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

// KuiklyMarkdown 1.0.10-internal still depends on com.tencent.kuikly 2.21.
// The app links com.tencent.kuikly-open 2.30, which uses the same packages.
configurations.configureEach {
    exclude(group = "com.tencent.kuikly")
    // KuiklyMarkdown AAR already embeds org.intellij.markdown classes used by
    // kuikly-compose-markdown's parser; keep one copy for the Android demo APK.
    exclude(group = "org.jetbrains", module = "markdown")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":KuiklyChatAndroid"))
    implementation(A2ui.core())
    implementation(A2ui.renderAndroid())

    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.appcompat:appcompat:1.3.1")

    implementation("com.squareup.picasso:picasso:2.71828")

    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.dynamicanimation:dynamicanimation:1.0.0")
}
