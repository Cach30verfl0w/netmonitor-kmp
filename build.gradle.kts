plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform) apply false
    alias(sharedLibs.plugins.android.library) apply false
    alias(sharedLibs.plugins.android.application) apply false
}

val refName = System.getenv("GITHUB_REF_NAME") ?: "local"
val refType = System.getenv("GITHUB_REF_TYPE") ?: "branch"

val baseVersion = sharedLibs.versions.netmonitor.get()
val projectVersion = when {
    refType == "tag" -> baseVersion
    refName == "main" -> "$baseVersion-SNAPSHOT"
    else -> "$baseVersion-$refName-dev"
}

allprojects {
    group = "net.cacheoverflow.netmonitor"
    version = projectVersion
}
