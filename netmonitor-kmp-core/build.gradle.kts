@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalAbiValidation::class)

import net.cacheoverflow.kotlinconventions.apacheLicense2
import net.cacheoverflow.kotlinconventions.withAndroidLibrary
import net.cacheoverflow.kotlinconventions.withJvm
import net.cacheoverflow.kotlinconventions.withMavenPublish
import net.cacheoverflow.kotlinconventions.withNative
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.Family

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.android.library)
    alias(sharedLibs.plugins.mavenPublish)
    alias(sharedLibs.plugins.kotlin.conventions)
    signing
}

withAndroidLibrary {
    lint.targetSdk = sharedLibs.versions.android.targetSdk.get().toInt()
}
withJvm()
withNative {
    binaries.sharedLib()
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("ios") { withIos() }
            group("macos") { withMacos() }
            group("native") {
                withMingw()
                withLinux()
                group("ios")
                group("macos")
            }
            group("nonAndroid") {
                withJvm()
                group("native")
            }
        }
    }

    abiValidation()

    sourceSets {
        sourceSets.all {
            compilerOptions {
                freeCompilerArgs.add("-Xcontext-parameters")
            }

            languageSettings {
                optIn("net.cacheoverflow.netmonitor.InternalNetMonitorAPI")
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }

        commonMain {
            dependencies {
                implementation(sharedLibs.compose.runtime.annotations)
                api(sharedLibs.kotlinx.coroutines.core)
            }
        }
        mingwMain {
            dependencies {
                implementation(sharedLibs.comInterop.core)
            }
        }
    }
}

val copyNativeBinariesToJar = tasks.register<Copy>("copyNativeBinariesToTar") {
    group = "natives"
    description = "Copies required native libraries to the jar output"
    into(layout.buildDirectory.dir("generated/native-resources"))
    kotlin.targets.filterIsInstance<KotlinNativeTarget>()
        .filter { it.konanTarget.family != Family.IOS }
        .forEach { target ->
            val sharedLibrary = requireNotNull(target.binaries.findSharedLib(NativeBuildType.RELEASE))
            dependsOn(sharedLibrary.linkTaskProvider)
            from(sharedLibrary.linkTaskProvider.map { it.outputFile }) {
                val arch = target.konanTarget.architecture.name.lowercase()
                val extension = sharedLibrary.outputFile.extension
                val family = when (val f = target.konanTarget.family) {
                    Family.MINGW -> "windows"
                    Family.OSX -> "macos"
                    else -> f.name.lowercase()
                }

                rename { "netmonitor-binaries/${family}_${arch}.$extension" }
            }
        }
}

kotlin {
    jvm {
        compilations.getByName("main").defaultSourceSet.resources.srcDir(copyNativeBinariesToJar)
    }
}

mavenPublishing {
    signAllPublications()
}

withMavenPublish {
    name = "netmonitor-kmp"
    description = " A reactive network monitoring library for Kotlin Multiplatform."
    url = "https://github.com/cach30verfl0w/netmonitor-kmp"
    apacheLicense2()
    developers {
        developer {
            id = "cach30verfl0w"
            name = "Cedric Hammes"
            url = "https://github.com/cach30verfl0w"
            email = "contact@cach30verfl0w.net"
        }
    }
    contributors {
        contributor {
            name = "KitsuneAlex"
            url = "https://karmakrafts.dev"
            email = "support@karmakrafts.dev"
        }
    }
    scm {
        url = "https://github.com/cach30verfl0w/netmonitor-kmp"
    }
}

signing {
    isRequired = false
}
