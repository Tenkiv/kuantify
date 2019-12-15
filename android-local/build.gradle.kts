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
import java.io.*
import java.util.*


plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.dokka")
    id("digital.wup.android-maven-publish") version Vof.androidMaven
    signing
}

android {
    compileSdkVersion(28)
    defaultConfig {
        minSdkVersion(26)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
    }

}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api(project(":android-core"))
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version = Vof.coroutinesX)
}

tasks {
    register<Jar>("sourcesJar") {
        from(kotlin.sourceSets["main"].kotlin)
        archiveClassifier.set("sources")
    }

    register<Jar>("javadocJar") {
        from(getByName<DokkaTask>("dokka"))
        archiveClassifier.set("javadoc")
    }

    getByName("assemble") {
        dependsOn("sourcesJar")
        dependsOn("javadocJar")
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

                from(components["android"])
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

                from(components["android"])
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

