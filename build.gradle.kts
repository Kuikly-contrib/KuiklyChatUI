plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application").version("8.4.0").apply(false)
    id("com.android.library").version("8.4.0").apply(false)
    kotlin("android").version("2.1.21").apply(false)
    kotlin("multiplatform").version("2.1.21").apply(false)
    kotlin("plugin.compose").version("2.1.21").apply(false)
    id("com.google.devtools.ksp").version("2.1.21-2.0.1").apply(false)

}

// KBA kotlinx artifacts pin an unpublished Kotlin dev build in their JS metadata.
// Keep JS publication resolution on the Kotlin version used by this project.
allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" &&
                requested.version == "2.0.21-dev-107"
            ) {
                useVersion("2.1.21")
            }
        }
    }
}

buildscript {
    dependencies {
        classpath(BuildPlugin.kuikly)
    }
}
