plugins {
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.androidApplication).apply(false)
}

val projectVersion = libs.versions.netmonitor.get()
allprojects {
    group = "dev.cacheoverflow.netmonitor"
    version = projectVersion
}
