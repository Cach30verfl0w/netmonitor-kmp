plugins {
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.androidLibrary).apply(false)
    alias(libs.plugins.androidApplication).apply(false)
}

val refName = System.getenv("GITHUB_REF_NAME") ?: "local"
val refType = System.getenv("GITHUB_REF_TYPE") ?: "branch"

val baseVersion = libs.versions.netmonitor.get()
val projectVersion = when {
    refType == "tag" -> baseVersion
    refName == "main" -> "$baseVersion-SNAPSHOT"
    else -> "$baseVersion-$refName-dev"
}

allprojects {
    group = "net.cacheoverflow.netmonitor"
    version = projectVersion
}
