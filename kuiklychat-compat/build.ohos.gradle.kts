plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
    `maven-publish`
}

apply(from = rootProject.file("gradle/publish-maven.gradle"))

kotlin {
    explicitApi()
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        publishLibraryVariants("release")
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    ohosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kuiklychat-core"))
                api(project(":kuiklychat-ai"))
                implementation("com.tencent.kuikly:core:${Version.getKuiklyOhosVersion()}")
                api("com.tencent.kuikly:compose:${Version.getKuiklyOhosVersion()}")
                implementation("com.tencent.kuikly:core-annotations:${Version.getKuiklyOhosVersion()}")
                implementation("com.tencent.kuiklybase:KuiklyMarkdown:1.0.4-internal-2.0.21-mini-009")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-KBA-002")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "com.tencent.kuiklybase.chat.compat"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets {
        named("main") {
            assets.srcDirs("${rootProject.projectDir}/kuiklychat-compose/src/commonMain/assets")
        }
    }

}

