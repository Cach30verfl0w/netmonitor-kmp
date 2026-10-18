pluginManagement {
    if (file("../kotlin-conventions").exists()) {
        includeBuild("../kotlin-conventions")
    }

    repositories {
        google {
            @Suppress("UnstableApiUsage")
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")
    id("net.cacheoverflow.kotlinconventions.settings-plugin").version("1.0.0")
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "netmonitor-kmp"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("netmonitor-kmp-core")
include("netmonitor-kmp-example")
include("netmonitor-kmp-example-android")
