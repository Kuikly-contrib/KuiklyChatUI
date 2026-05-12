pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/tencentvideo")
        }
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/tmm-snapshot")
        }
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/kuikly")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/tencentvideo")
        }
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/tmm-snapshot")
        }
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/kuikly")
        }
    }
}

rootProject.name = "KuiklyChat"

val buildFileName = "build.ohos.gradle.kts"
rootProject.buildFileName = buildFileName

include(":androidApp")
include(":shared")
include(":KuiklyChat")
project(":shared").buildFileName = buildFileName
project(":KuiklyChat").buildFileName = "build.ohos.gradle.kts"