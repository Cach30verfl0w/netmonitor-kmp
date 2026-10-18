@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform)
    alias(sharedLibs.plugins.compose.multiplatform)
    alias(sharedLibs.plugins.compose.compiler)
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

kotlin {
    jvmToolchain(sharedLibs.versions.jvmTarget.get().toInt())
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(sharedLibs.versions.jvmTarget.get()))
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.netmonitorKmpCore)
                implementation(sharedLibs.compose.foundation)
                implementation(sharedLibs.compose.runtime)
                implementation(sharedLibs.compose.ui)
                implementation(sharedLibs.compose.material3)
            }
        }
        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose {
    desktop {
        application {
            mainClass = "${rootProject.group}.example.Main"
            jvmArgs("--enable-native-access=ALL-UNNAMED")
        }
    }
}
