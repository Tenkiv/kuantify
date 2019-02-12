plugins {
    `maven-publish`
    signing
}

val isSnapshot: Boolean = version.toString().contains("SNAPSHOT")

val modulesList = listOf(
    "${projectDir.absolutePath}/core",
    "${projectDir.absolutePath}/android-core",
    "${projectDir.absolutePath}/android-local",
    "${projectDir.absolutePath}/learning"
)

tasks {
    getByName<Upload>("uploadArchives") {
        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    withGroovyBuilder {
                        "repository"("url" to uri()) {
                            "authentication"("userName", "bob")
                        }
                        "snapshotRepository"("url" to uri("$buildDir/m2/snapshots"))
                    }
                    pom.project {
                        withGroovyBuilder {
                            "parent" {
                                "groupId"("org.gradle")
                                "artifactId"("kotlin-dsl")
                                "version"("1.0")
                            }
                            "licenses" {
                                "license" {
                                    "name"("The Apache Software License, Version 2.0")
                                    "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                    "distribution"("repo")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}