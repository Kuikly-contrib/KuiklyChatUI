plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application").version("8.4.0").apply(false)
    id("com.android.library").version("8.4.0").apply(false)
    kotlin("android").version("2.0.21-KBA-010").apply(false)
    kotlin("multiplatform").version("2.0.21-KBA-010").apply(false)
    kotlin("plugin.compose").version("2.0.21-KBA-010").apply(false)
    id("com.google.devtools.ksp").version("2.0.21-1.0.27").apply(false)

}