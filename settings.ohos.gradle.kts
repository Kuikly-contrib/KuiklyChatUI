pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven-tencent/")
        }
    }
}

rootProject.name = "KuiklyChat"

val buildFileName = "build.ohos.gradle.kts"
rootProject.buildFileName = buildFileName

include(":shared")
include(":kuiklychat-core")
include(":kuiklychat-compose")
include(":kuiklychat-compat")
include(":kuiklychat-ai")
include(":kuiklychat-testing")

listOf(
    ":shared",
    ":kuiklychat-core",
    ":kuiklychat-compose",
    ":kuiklychat-compat",
    ":kuiklychat-ai",
    ":kuiklychat-testing",
).forEach { name ->
    project(name).buildFileName = buildFileName
}