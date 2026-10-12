pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/kuikly")
        }
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/tencent_public")
        }
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/tencentvideo")
        }
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/tmm-snapshot")
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
            url = uri("https://mirrors.tencent.com/repository/maven/kuikly")
        }
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/tencent_public")
        }
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/tencentvideo")
        }
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/tmm-snapshot")
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