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

@file:Suppress("KDocMissingDocumentation", "PublicApiImplicitType")
import java.io.FileInputStream
import java.util.*

buildscript {
    repositories {
        mavenCentral()
        google()
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-serialization:${Vof.kotlin}")
    }
}

repositories {
    mavenCentral()
    jcenter()
    google()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

plugins {
    kotlin("multiplatform") version Vof.kotlin
    id("org.jetbrains.kotlin.plugin.serialization") version Vof.kotlin
    id("org.jetbrains.dokka") version Vof.dokka
    id("maven-publish")
    signing
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

kotlin {
    jvm()

    sourceSets {
        /**
         * commonMain == core-common artifact
         */
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        /**
         * jvmMain == core-jvm artifact
         */

        jvm {
            val main by compilations.getting {
                kotlinOptions {
                    jvmTarget = "1.8"
                }

                dependencies {
                    implementation(kotlin("stdlib-jdk8", Vof.kotlin))

                    //General kotlin utilities
                    api("org.tenkiv.coral:coral-jvm:${Vof.coral}")
                    api("io.arrow-kt:arrow-core:${Vof.arrow}")
                    implementation(kotlin("reflect", Vof.kotlin))

                    //Coroutines
                    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Vof.coroutinesX}")
                    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Vof.coroutinesX}")
                    api("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Vof.coroutinesX}")

                    //Logging
                    implementation("io.github.microutils:kotlin-logging:${Vof.kotlinLogging}")

                    //Serialization
                    api("org.jetbrains.kotlinx:kotlinx-serialization-runtime:${Vof.serializationX}")

                    //Units of measurement
                    api("org.tenkiv.physikal:physikal-complete-units:${Vof.physikal}")

                    //ktor
                    implementation("io.ktor:ktor-server-core:${Vof.ktor}")
                    implementation("io.ktor:ktor-websockets:${Vof.ktor}")
                    implementation("io.ktor:ktor-server-sessions:${Vof.ktor}")

                    implementation("io.ktor:ktor-client-core-jvm:${Vof.ktor}")
                    implementation("io.ktor:ktor-client-websockets-jvm:${Vof.ktor}")
                }
            }

            val test by compilations.getting {
                dependencies {
                    implementation(kotlin("reflect"))
                    implementation(kotlin("test"))
                    implementation("org.spekframework.spek2:spek-dsl-jvm:${Vof.spek}")
                    implementation("io.mockk:mockk:${Vof.mockk}")
                    implementation("io.ktor:ktor-server-test-host:${Vof.ktor}")
                    implementation("io.ktor:ktor-client-cio:${Vof.ktor}")
                    runtimeOnly("org.spekframework.spek2:spek-runner-junit5:${Vof.spek}")
                    runtimeOnly("org.junit.platform:junit-platform-launcher:${Vof.junitPlatform}")
                }
            }

            tasks {
                withType<Test> {
                    outputs.upToDateWhen { false }
                    useJUnitPlatform {
                        includeEngines("spek2")
                    }
                    testLogging.showStandardStreams = true
                    maxHeapSize = "1g"
                }

                register<Jar>("sourcesJar") {
                    archiveClassifier.set("sources")
                    from(kotlin.sourceSets.commonTest)
                }

                register<Jar>("javadocJar") {
                    archiveClassifier.set("javadoc")
                    from(getByName("dokka"))
                }
            }
        }
    }

    publishing {
        publications.withType<MavenPublication>().apply {
            val jvm by getting {
                artifactId = "core-jvm"
                artifact(tasks.getByName("sourcesJar"))
                artifact(tasks.getByName("javadocJar"))
            }

            val metadata by getting {
                artifactId = "core-common"
            }
        }.forEach {
            version = if (isRelease) project.version.toString() else "${project.version}-SNAPSHOT"

            it.pom {
                name.set(project.name)
                description.set(Info.pomDescription)
                url.set(if (isRelease) Info.projectUrl else System.getenv("CI_PROJECT_URL"))

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
                    connection.set(if (isRelease) Info.projectCloneUrl else System.getenv("CI_REPOSITORY_URL"))
                    url.set(if (isRelease) Info.projectUrl else System.getenv("CI_PROJECT_URL"))
                }
            }

            signing {
                if (isRelease) sign(it)
            }
        }

        repositories {
            maven {
                url = uri(if (isRelease) Info.sonatypeReleaseRepoUrl else Info.sonatypeSnapshotRepoUrl)

                credentials {
                    username = if (isRelease) properties.getProperty("MAVEN_USER") else System.getenv("MAVEN_REPO_USER")
                    password = if (isRelease) properties.getProperty("MAVEN_PASSWORD") else System.getenv("MAVEN_REPO_PASSWORD")
                }
            }
        }
    }
}