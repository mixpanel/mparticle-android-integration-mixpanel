import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.plugins.signing.SigningExtension

/**
 * Convention plugin for publishing Android libraries to Maven Central.
 *
 * Uses OSSRH Staging API - compatible with built-in maven-publish plugin.
 * Artifacts are staged and require manual publishing via the Central Portal.
 *
 * Required environment variables for publishing (set as GitHub Actions secrets):
 * - MAVEN_CENTRAL_USERNAME: Maven Central Portal username (from User Token)
 * - MAVEN_CENTRAL_PASSWORD: Maven Central Portal password (from User Token)
 * - SIGNING_KEY: GPG private key (ASCII-armored)
 * - SIGNING_PASSWORD: GPG key passphrase
 *
 * Required project properties (set in gradle.properties):
 * - GROUP: Maven group ID (e.g., com.mixpanel.android)
 * - POM_ARTIFACT_ID: Maven artifact ID
 * - VERSION_NAME: Version string
 * - POM_NAME: Human-readable name
 * - POM_DESCRIPTION: Library description
 * - POM_URL: Project URL
 * - POM_SCM_URL: SCM URL
 * - POM_SCM_CONNECTION: SCM connection string
 * - POM_SCM_DEV_CONNECTION: SCM developer connection string
 * - POM_LICENCE_NAME: License name
 * - POM_LICENCE_URL: License URL
 * - POM_DEVELOPER_ID: Developer ID
 * - POM_DEVELOPER_NAME: Developer name
 * - POM_DEVELOPER_EMAIL: Developer email
 */
class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("maven-publish")
            pluginManager.apply("signing")

            registerJarTasks()

            afterEvaluate {
                configurePublishing()
                configureSigning()
                configureTaskDependencies()
                configurePublishTasks()
            }
        }
    }

    private fun Project.registerJarTasks() {
        tasks.register<Jar>("sourcesJar") {
            archiveClassifier.set("sources")
            from("src/main/java", "src/main/kotlin")
        }

        // Maven Central requires a javadoc jar; include sources as documentation
        // until Dokka is added for proper KDoc generation
        tasks.register<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
        }
    }

    private fun Project.configureTaskDependencies() {
        val bundleTask = "bundleReleaseAar"

        // Release publication
        tasks.named("generateMetadataFileForReleasePublication").configure {
            dependsOn(bundleTask, "sourcesJar", "javadocJar")
        }
        tasks.named("generatePomFileForReleasePublication").configure {
            dependsOn(bundleTask)
        }

        // Snapshot publication
        tasks.named("generateMetadataFileForSnapshotPublication").configure {
            dependsOn(bundleTask, "sourcesJar", "javadocJar")
        }
        tasks.named("generatePomFileForSnapshotPublication").configure {
            dependsOn(bundleTask)
        }
    }

    private fun Project.configurePublishTasks() {
        // Disable cross-publication tasks
        tasks.named("publishReleasePublicationToMavenLocal").configure { enabled = false }
        tasks.named("publishReleasePublicationToSnapshotRepository").configure { enabled = false }
        tasks.named("publishSnapshotPublicationToMavenLocal").configure { enabled = false }
        tasks.named("publishSnapshotPublicationToOssrhStagingRepository").configure { enabled = false }

        // Validate and create alias for release publishing
        tasks.named("publishReleasePublicationToOssrhStagingRepository").configure {
            doFirst {
                val missingEnvVars = listOf(
                    "MAVEN_CENTRAL_USERNAME",
                    "MAVEN_CENTRAL_PASSWORD",
                    "SIGNING_KEY",
                    "SIGNING_PASSWORD"
                ).filter { System.getenv(it).isNullOrBlank() }

                if (missingEnvVars.isNotEmpty()) {
                    throw org.gradle.api.GradleException(
                        "Cannot publish to Maven Central. Missing required environment variables: ${missingEnvVars.joinToString()}"
                    )
                }
            }
        }

        tasks.register("publishRelease") {
            group = "publishing"
            description = "Publishes release to Maven Central staging (requires manual publish in Portal)"
            dependsOn("publishReleasePublicationToOssrhStagingRepository")
        }

        // Validate and create alias for snapshot publishing
        tasks.named("publishSnapshotPublicationToSnapshotRepository").configure {
            doFirst {
                val missingEnvVars = listOf(
                    "MAVEN_CENTRAL_USERNAME",
                    "MAVEN_CENTRAL_PASSWORD"
                ).filter { System.getenv(it).isNullOrBlank() }

                if (missingEnvVars.isNotEmpty()) {
                    throw org.gradle.api.GradleException(
                        "Cannot publish snapshot. Missing required environment variables: ${missingEnvVars.joinToString()}"
                    )
                }
            }
        }

        tasks.register("getSnapshotVersion") {
            group = "publishing"
            description = "Prints the snapshot version"
            doLast {
                val versionName = propertyOrDefault("VERSION_NAME", project.version.toString())
                println(computeNextSnapshotVersion(versionName))
            }
        }

        tasks.register("publishSnapshot") {
            group = "publishing"
            description = "Publishes snapshot to Maven Central"
            dependsOn("publishSnapshotPublicationToSnapshotRepository")
        }

        // Local staging tasks for inspection before publishing
        tasks.register("stageRelease") {
            group = "publishing"
            description = "Stages release locally for inspection (build/staging)"
            dependsOn("publishReleasePublicationToLocalStagingRepository")
            doLast {
                println("Release staged to: ${layout.buildDirectory.get()}/staging")
            }
        }

        tasks.register("stageSnapshot") {
            group = "publishing"
            description = "Stages snapshot locally for inspection (build/staging)"
            dependsOn("publishSnapshotPublicationToLocalStagingRepository")
            doLast {
                println("Snapshot staged to: ${layout.buildDirectory.get()}/staging")
            }
        }
    }

    private fun Project.configurePublishing() {
        val groupId = propertyOrDefault("GROUP", "com.mixpanel.android")
        val artifactId = propertyOrDefault("POM_ARTIFACT_ID", project.name)
        val versionName = propertyOrDefault("VERSION_NAME", project.version.toString())

        extensions.configure<PublishingExtension> {
            publications {
                create<MavenPublication>("release") {
                    this.groupId = groupId
                    this.artifactId = artifactId
                    this.version = versionName

                    afterEvaluate {
                        artifact(tasks.named("bundleReleaseAar"))
                    }
                    artifact(tasks.named("sourcesJar"))
                    artifact(tasks.named("javadocJar"))

                    configurePom(project)
                    configureDependencies(project)
                }

                create<MavenPublication>("snapshot") {
                    this.groupId = groupId
                    this.artifactId = artifactId
                    this.version = computeNextSnapshotVersion(versionName)

                    afterEvaluate {
                        artifact(tasks.named("bundleReleaseAar"))
                    }
                    artifact(tasks.named("sourcesJar"))
                    artifact(tasks.named("javadocJar"))

                    configurePom(project)
                    configureDependencies(project)
                }
            }

            repositories {
                maven {
                    name = "localStaging"
                    url = uri(layout.buildDirectory.dir("staging"))
                }

                maven {
                    name = "ossrhStaging"
                    url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
                    credentials {
                        username = System.getenv("MAVEN_CENTRAL_USERNAME") ?: ""
                        password = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: ""
                    }
                }

                maven {
                    name = "snapshot"
                    url = uri("https://central.sonatype.com/repository/maven-snapshots/")
                    credentials {
                        username = System.getenv("MAVEN_CENTRAL_USERNAME") ?: ""
                        password = System.getenv("MAVEN_CENTRAL_PASSWORD") ?: ""
                    }
                }
            }
        }
    }

    private fun MavenPublication.configureDependencies(project: Project) {
        pom.withXml {
            val dependenciesNode = asNode().appendNode("dependencies")
            val added = mutableSetOf<String>()

            fun addDependencies(configName: String, scope: String) {
                project.configurations.findByName(configName)?.dependencies?.forEach { dep ->
                    val key = "${dep.group}:${dep.name}"
                    if (dep.group != null && key !in added) {
                        added.add(key)
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", dep.group)
                        dependencyNode.appendNode("artifactId", dep.name)
                        dependencyNode.appendNode("version", dep.version ?: "")
                        dependencyNode.appendNode("scope", scope)
                    }
                }
            }

            addDependencies("api", "compile")
            addDependencies("implementation", "runtime")
        }
    }

    private fun MavenPublication.configurePom(project: Project) {
        pom {
            name.set(project.propertyOrDefault("POM_NAME", project.name))
            description.set(project.propertyOrDefault("POM_DESCRIPTION"))
            url.set(project.propertyOrDefault("POM_URL"))

            licenses {
                license {
                    name.set(project.propertyOrDefault("POM_LICENCE_NAME", "The Apache License, Version 2.0"))
                    url.set(project.propertyOrDefault("POM_LICENCE_URL", "https://www.apache.org/licenses/LICENSE-2.0.txt"))
                }
            }

            developers {
                developer {
                    id.set(project.propertyOrDefault("POM_DEVELOPER_ID", "mixpanel"))
                    name.set(project.propertyOrDefault("POM_DEVELOPER_NAME", "Mixpanel"))
                    email.set(project.propertyOrDefault("POM_DEVELOPER_EMAIL"))
                }
            }

            scm {
                url.set(project.propertyOrDefault("POM_SCM_URL"))
                connection.set(project.propertyOrDefault("POM_SCM_CONNECTION"))
                developerConnection.set(project.propertyOrDefault("POM_SCM_DEV_CONNECTION"))
            }
        }
    }

    private fun Project.configureSigning() {
        val signingKey = System.getenv("SIGNING_KEY")
        val signingPassword = System.getenv("SIGNING_PASSWORD")

        if (signingKey.isNullOrBlank()) {
            return
        }

        extensions.configure<SigningExtension> {
            useInMemoryPgpKeys(signingKey, signingPassword)

            val publishing = extensions.getByType(PublishingExtension::class.java)
            sign(publishing.publications.getByName("release"))

            isRequired = !propertyOrDefault("VERSION_NAME").endsWith("SNAPSHOT")
        }
    }
}

private fun Project.propertyOrDefault(name: String, default: String = ""): String =
    findProperty(name)?.toString() ?: default

private fun computeNextSnapshotVersion(version: String): String {
    val parts = version.split(".")
    if (parts.size < 3) {
        return "$version.1-SNAPSHOT"
    }
    val major = parts[0]
    val minor = parts[1]
    val patch = parts[2].replace(Regex("[^0-9].*"), "").toIntOrNull() ?: 0
    return "$major.$minor.${patch + 1}-SNAPSHOT"
}
