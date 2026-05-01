@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.Family

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.mavenPublish)
}


kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
    androidTarget()
    listOf(iosSimulatorArm64(), iosArm64(), iosX64(), linuxX64(), linuxArm64(), macosArm64()).forEach {
        it.binaries.sharedLib()
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
        }
    }

    applyDefaultHierarchyTemplate {
        common {
            group("nonAndroid") {
                withJvm()
                group("native") {
                    group("linux") {
                        withLinuxX64()
                        withLinuxArm64()
                    }
                    group("apple") {
                        withIos()
                        withMacos()
                    }
                }
            }
        }
    }

    abiValidation {
        enabled = true
    }

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
                implementation(libs.compose.runtime.annotations)
                api(libs.kotlinx.coroutines.core)
            }
        }
    }
}

val copyNativeBinariesToJar = tasks.register<Copy>("copyNativeBinariesToTar") {
    into(layout.buildDirectory.dir("generated/native-resources"))
    kotlin.targets.filterIsInstance<KotlinNativeTarget>().filter { it.konanTarget.family != Family.IOS }.forEach { target ->
        val sharedLibrary = requireNotNull(target.binaries.findSharedLib(NativeBuildType.RELEASE))
        dependsOn(sharedLibrary.linkTaskProvider)
        from(sharedLibrary.linkTaskProvider.map { it.outputFile }) {
            val arch = target.konanTarget.architecture.name.lowercase()
            val extension = sharedLibrary.outputFile.extension
            val family = when(val f = target.konanTarget.family) {
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

android {
    namespace = group.toString()
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), name, version.toString())

    pom {
        name = "netmonitor-kmp"
        description = " A reactive network monitoring library for Kotlin Multiplatform."
        url = "https://github.com/cach30verfl0w/netmonitor-kmp"
        licenses {
            license {
                name = "Apache License 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "cach30verfl0w"
                name = "Cedric Hammes"
                url = "https://github.com/cach30verfl0w"
            }
        }
        scm {
            url = this@pom.url
        }
    }
}
