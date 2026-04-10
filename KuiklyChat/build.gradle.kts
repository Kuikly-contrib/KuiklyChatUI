plugins {
    kotlin("multiplatform")
    id("com.android.library")
    `maven-publish`
}

// 从 Gradle 参数读取发布配置（优先读取命令行 -P 参数，回退到 gradle.properties 大写风格）
val mavenVersion: String = findProperty("mavenVersion") as? String
    ?: findProperty("MAVEN_VERSION") as? String
    ?: "1.0.0"
val groupId: String = findProperty("groupId") as? String
    ?: findProperty("GROUP_ID") as? String
    ?: "com.tencent.kuiklybase"
val mavenRepoUrl: String = findProperty("mavenRepoUrl") as? String
    ?: findProperty("MAVEN_REPO_URL") as? String
    ?: "https://mirrors.tencent.com/repository/maven/kuikly-open/"
val mavenUsername: String = findProperty("mavenUsername") as? String
    ?: findProperty("MAVEN_USERNAME") as? String
    ?: ""
val mavenPassword: String = findProperty("mavenPassword") as? String
    ?: findProperty("MAVEN_PASSWORD") as? String
    ?: ""

group = groupId
version = mavenVersion

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
        publishLibraryVariants("release")
    }

    js(IR) {
        browser()
        binaries.executable()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.tencent.kuikly-open:core:${Version.getKuiklyVersion()}")
                implementation("com.tencent.kuikly-open:core-annotations:${Version.getKuiklyVersion()}")
                // KuiklyMarkdown — Markdown 渲染组件
                implementation("com.tencent.kuiklybase:KuiklyMarkdown:1.0.4-2.0.21")
                // KuiklyMarkdown 所需的依赖
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-KBA-002")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1-KBA-003")
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
    namespace = "com.tencent.kuiklybase.chat"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

publishing {
    repositories {
        maven {
            url = uri(mavenRepoUrl)
            credentials {
                username = mavenUsername
                password = mavenPassword
            }
        }
    }
}
