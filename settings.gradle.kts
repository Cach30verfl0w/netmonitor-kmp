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
}

dependencyResolutionManagement {
    versionCatalogs {
        create("sharedLibs") {
            val localCatalog = file("../kotlin-conventions/gradle/libs.versions.toml")
            if (localCatalog.exists()) {
                from(files(localCatalog))
            } else {
                from("net.cacheoverflow.kotlinconventions:shared-catalog:1.0.0")
            }
        }
    }

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
