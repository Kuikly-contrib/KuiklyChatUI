plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
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

    macosX64()
    macosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.tencent.kuikly:core:${Version.getKuiklyVersion()}")
                implementation("com.tencent.kuikly:core-annotations:${Version.getKuiklyVersion()}")
                // Compose DSL 支持
                api("com.tencent.kuikly:compose:${Version.getKuiklyVersion()}")
                // KuiklyMarkdown — Markdown 渲染组件
                implementation("com.tencent.kuiklybase:KuiklyMarkdown:1.0.6-internal-2.1.21")
                // KuiklyMarkdown 所需的依赖
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("com.tencent.kuiklybase:KuiklyMarkdownCompose:1.0.10-internal")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                // 多平台（jvm/js/native 全支持），不会破坏非 JVM target 的测试编译。
                // 用于测试 KuiklyAI 的 Flow 流式与挂起逻辑。
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            }
        }
        val androidMain by getting
        val jsMain by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
        val macosX64Main by getting
        val macosArm64Main by getting
        val macosMain by creating {
            dependsOn(commonMain)
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "com.tencent.kuiklybase.chat"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    sourceSets {
        named("main") {
            assets.srcDirs("src/commonMain/assets")
        }
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
