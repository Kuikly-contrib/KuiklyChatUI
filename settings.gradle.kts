pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/repository/maven/kuikly")
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
    }
}

rootProject.name = "KuiklyChat"
include(":androidApp")
include(":shared")
include(":KuiklyChat")
include(":KuiklyChatAndroid")
include(":h5App")
include(":miniApp")