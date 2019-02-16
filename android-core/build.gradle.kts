import org.jetbrains.dokka.gradle.*

/*
 * Copyright 2019 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

plugins {
    kotlin("jvm")
    id("kotlinx-serialization")
    id("org.jetbrains.dokka")
    `maven-publish`
    java
}

dependencies {
    compile(project(":core"))
}

tasks {
    register<Jar>("sourcesJar") {
        from(kotlin.sourceSets["main"].kotlin)
        classifier = "sources"
    }

    register<Jar>("javadocJar") {
        from(tasks["dokka"])
        classifier = "javadoc"
    }

    getByName("build") {
        dependsOn("sourcesJar")
        dependsOn("javadocJar")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven-${project.name}") {
            groupId = "org.tenkiv.kuantify"
            artifactId = "kuantify-${project.name}"
            version = project.version.toString()

            from(components["java"])

            for (file in project.fileTree("build/libs").files) {
                when {
                    file.name.contains("javadoc") -> {
                        val a = artifact(file)
                        a.classifier = "javadoc"
                    }
                    file.name.contains("sources") -> {
                        val a = artifact(file)
                        a.classifier = "sources"
                    }
                }
            }

            pom {
                name.set(project.name)
                description.set(Info.pomDescription)
                url.set(System.getenv("CI_PROJECT_URL"))
                licenses {
                    license {
                        name.set(Info.pomLicense)
                        url.set(Info.pomLicenseUrl)
                    }
                }
                organization {
                    name.set(Info.pomOrg)
                }
                scm {
                    connection.set(System.getenv("CI_REPOSITORY_URL"))
                    url.set(System.getenv("CI_PROJECT_URL"))
                }
            }
        }
    }
    repositories {
        maven {
            // change URLs to point to your repos, e.g. http://my.org/repo
            val releasesRepoUrl = uri(Info.sonatypeReleaseRepoUrl)
            val snapshotsRepoUrl = uri(Info.sonatypeSnapshotRepoUrl)
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getenv("MAVEN_REPO_USER")
                password = System.getenv("MAVEN_REPO_PASSWORD")
            }
        }
    }
}