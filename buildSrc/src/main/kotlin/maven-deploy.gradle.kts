plugins {
    `java-library`
    `maven-publish`
    signing
}

val modulesList = listOf(
    "${projectDir.absolutePath}/core",
    "${projectDir.absolutePath}/android-core",
    "${projectDir.absolutePath}/android-local",
    "${projectDir.absolutePath}/learning"
)

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.tenkiv.kuantify"
            artifactId = "kuantify"
            version = project.version.toString()

            //TODO: get artifacts here
            artifact(components["java"])
            println(components["java"])
            from(components["java"])

            pom {
                name.set(project.name)
                description.set("API for easily managing, controlling, and interfacing with different data acquisition systems.")
                url.set("https://gitlab.com/tenkiv/software/kuantify")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("zjuhasz")
                        name.set("Zachary Juhasz")
                        email.set("zjuhasz@protonmail.com")
                    }
                    developer {
                        id.set("skern")
                        name.set("Shannon Kern")
                        email.set("me@shankern.com")
                    }
                }
                organization {
                    name.set("Tenkiv, Inc.")
                }
                scm {
                    connection.set("git@gitlab.com:tenkiv/software/kuantify.git")
                    url.set("https://gitlab.com/tenkiv/software/kuantify")
                }
            }
        }
    }
    repositories {
        maven {
            // change URLs to point to your repos, e.g. http://my.org/repo
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getenv("MAVEN_REPO_USER")
                password = System.getenv("MAVEN_REPO_PASSWORD")
            }
        }
    }

}
