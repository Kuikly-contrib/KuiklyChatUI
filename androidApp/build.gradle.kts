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
        // Key stays in gitignored local.properties. An empty value keeps the offline sample card.
        buildConfigField("String", "KIMI_API_KEY", localKimiSetting("KIMI_API_KEY").asBuildConfigString())
        buildConfigField("String", "KIMI_MODEL", localKimiSetting("KIMI_MODEL").ifEmpty { "kimi-k2.6" }.asBuildConfigString())
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
