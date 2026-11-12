pluginManagement {
    // Keep in sync with Version.getKuiklyVersion() in buildSrc.
    val kuiklyPluginVersion = "2.30.0-2.1.21"
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.tencent.kuikly-open.kuikly") {
                useModule("com.tencent.kuikly-open:core-gradle-plugin:$kuiklyPluginVersion")
            }
        }
    }
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
