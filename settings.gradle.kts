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
    }
}

rootProject.name = "KuiklyChat"
include(":androidApp")
include(":shared")
include(":KuiklyChat")
include(":h5App")
include(":miniApp")