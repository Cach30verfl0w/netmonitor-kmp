import org.gradle.api.credentials.HttpHeaderCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.authentication.http.HttpHeaderAuthentication

plugins {
    alias(sharedLibs.plugins.kotlin.multiplatform) apply false
    alias(sharedLibs.plugins.android.library) apply false
    alias(sharedLibs.plugins.android.application) apply false
}

val baseVersion = sharedLibs.versions.netmonitor.get()
val ciPipelineId = providers.environmentVariable("CI_PIPELINE_ID").orNull
val ciCommitTag = providers.environmentVariable("CI_COMMIT_TAG").orNull
val projectVersion = when {
    ciCommitTag != null -> baseVersion
    ciPipelineId != null -> "$baseVersion-SNAPSHOT+$ciPipelineId"
    else -> baseVersion
}

allprojects {
    group = "net.cacheoverflow.netmonitor"
    version = projectVersion

    pluginManager.withPlugin("maven-publish") {
        val gitLabApiUrl = providers.environmentVariable("CI_API_V4_URL").orNull
        val gitLabProjectId = providers.environmentVariable("CI_PROJECT_ID").orNull
        val gitLabJobToken = providers.environmentVariable("CI_JOB_TOKEN").orNull

        if (gitLabApiUrl != null && gitLabProjectId != null && gitLabJobToken != null) {
            extensions.configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "GitLab"
                        url = uri("$gitLabApiUrl/projects/$gitLabProjectId/packages/maven")
                        credentials(HttpHeaderCredentials::class) {
                            name = "Job-Token"
                            value = gitLabJobToken
                        }
                        authentication {
                            create("header", HttpHeaderAuthentication::class)
                        }
                    }
                }
            }
        }
    }
}
