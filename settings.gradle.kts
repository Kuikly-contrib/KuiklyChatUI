pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
        maven {
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
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
            url = uri("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        }
    }
}

providers.environmentVariable("KUIKLY_UI_SOURCE").orNull
    ?.takeIf { file(it).isDirectory }
    ?.let { kuiklyUiSource ->
        includeBuild(kuiklyUiSource) {
            dependencySubstitution {
                substitute(module("com.tencent.kuikly-open:core"))
                    .using(project(":core"))
                substitute(module("com.tencent.kuikly-open:core-render-android"))
                    .using(project(":core-render-android"))
            }
        }
    }

rootProject.name = "KuiklyChat"
include(":androidApp")
include(":shared")
include(":kuiklychat-core")
include(":kuiklychat-compose")
include(":kuiklychat-compat")
include(":kuiklychat-ai")
include(":kuiklychat-testing")
include(":KuiklyChatAndroid")
include(":h5App")
include(":miniApp")
