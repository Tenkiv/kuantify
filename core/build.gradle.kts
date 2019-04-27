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

import org.jetbrains.dokka.gradle.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*
import java.util.*

plugins {
    kotlin("jvm")
    java
    id("kotlinx-serialization")
    id("org.jetbrains.dokka")
    `maven-publish`
    signing
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    //General kotlin utilities
    compile(group = "org.tenkiv.coral", name = "coral", version = Vof.coral)
    compile(group = "io.arrow-kt", name = "arrow-core", version = Vof.arrow)
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-reflect")

    //Coroutines
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = Vof.coroutinesX)
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-jdk8", version = Vof.coroutinesX)
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-slf4j", version = Vof.coroutinesX)

    //Logging
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = Vof.logging)

    //Serialization
    compile(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-serialization-runtime",
        version = Vof.serializationX
    )

    //Units of measurement
    compile(group = "org.tenkiv.physikal", name = "complete-units", version = Vof.physikal)

    //ktor
    implementation(group = "io.ktor", name = "ktor-server-core", version = Vof.ktor)
    implementation(group = "io.ktor", name = "ktor-websockets", version = Vof.ktor)
    implementation(group = "io.ktor", name = "ktor-server-sessions", version = Vof.ktor)

    implementation(group = "io.ktor", name = "ktor-client-core", version = Vof.ktor)
    implementation(group = "io.ktor", name = "ktor-client-websocket", version = Vof.ktor)


    //Test
    testImplementation(kotlin("reflect", Vof.kotlin))
    testImplementation(kotlin("test", Vof.kotlin))

    testImplementation(group = "org.spekframework.spek2", name = "spek-dsl-jvm", version = Vof.spek) {
        exclude(group = "org.jetbrains.kotlin")
    }

    testRuntimeOnly(group = "org.spekframework.spek2", name = "spek-runner-junit5", version = Vof.spek) {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.junit.platform")
    }

    testRuntimeOnly(group = "org.junit.platform", name = "junit-platform-launcher", version = Vof.junitPlatform) {
        because("Needed to run tests IDEs that bundle an older version")
    }

    testImplementation(gradleTestKit())

    testImplementation(group = "io.mockk", name = "mockk", version = Vof.mockk)

    testImplementation(group = "io.ktor", name = "ktor-server-test-host", version = Vof.ktor)
    testImplementation(group = "io.ktor", name = "ktor-client-cio", version = Vof.ktor)

}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }

        maxHeapSize = "4g"
    }

    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(kotlin.sourceSets["main"].kotlin)
    }

    register<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
        from(getByName<DokkaTask>("dokka"))
    }
}

val isRelease = !version.toString().endsWith("SNAPSHOT")
val properties = Properties()
val propertiesFile = File(rootDir, "local.properties")
if (propertiesFile.canRead()) {
    properties.load(FileInputStream(propertiesFile))
}

extra["signing.keyId"] = properties.getProperty("SIGNING_KEYID")
extra["signing.secretKeyRingFile"] = properties.getProperty("SIGNING_SECRETKEYRINGFILE")
extra["signing.password"] = properties.getProperty("SIGNING_KEYPASSWORD")

publishing {
    publications {
        if (isRelease) {
            create<MavenPublication>("maven-${project.name}") {
                groupId = "org.tenkiv.kuantify"
                artifactId = "kuantify-${project.name}"
                version = project.version.toString()

                from(components["java"])
                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])

                pom {
                    name.set(project.name)
                    description.set(Info.pomDescription)
                    url.set(Info.projectUrl)
                    licenses {
                        license {
                            name.set(Info.pomLicense)
                            url.set(Info.pomLicenseUrl)
                        }
                    }
                    developers {
                        developer {
                            email.set(Info.projectDevEmail)
                        }
                    }
                    organization {
                        name.set(Info.pomOrg)
                    }
                    scm {
                        connection.set(Info.projectCloneUrl)
                        url.set(Info.projectUrl)
                    }
                }
            }
        } else {
            create<MavenPublication>("maven-${project.name}-snapshot") {
                groupId = "org.tenkiv.kuantify"
                artifactId = "kuantify-${project.name}"
                version = project.version.toString()

                from(components["java"])
                artifact(tasks["sourcesJar"])
                artifact(tasks["javadocJar"])

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
                    developers {
                        developer {
                            email.set(Info.projectDevEmail)
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
    }
    repositories {
        maven {
            val releasesRepoUrl = uri(Info.sonatypeReleaseRepoUrl)
            val snapshotsRepoUrl = uri(Info.sonatypeSnapshotRepoUrl)
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                if (isRelease) {
                    username = properties.getProperty("MAVEN_USER")
                    password = properties.getProperty("MAVEN_PASSWORD")
                } else {
                    username = System.getenv("MAVEN_REPO_USER")
                    password = System.getenv("MAVEN_REPO_PASSWORD")
                }
            }
        }
    }
}

signing {
    if (isRelease) {
        sign(publishing.publications["maven-${project.name}"])
    }
}

