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

plugins {
    base
    kotlin("jvm") version Vof.kotlin apply false
    kotlin("android") version Vof.kotlin apply false
    kotlin("android.extensions") version Vof.kotlin apply false
    id("kotlinx-serialization") version Vof.kotlin apply false
    id("org.jetbrains.dokka") version Vof.dokka apply false
    signing
}

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Vof.androidGradle}")
//        classpath(kotlin("gradle-plugin", version = "1.3.10"))
    }
}

subprojects {
    buildscript {
        repositories {
            mavenCentral()
            jcenter()
            google()
        }
    }

    repositories {
        mavenCentral()
        jcenter()
        google()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://kotlin.bintray.com/ktor")
        maven(url = "https://kotlin.bintray.com/kotlinx")
    }
}

val projectList = listOf(
    project(":core"),
    project(":android-core"),
    project(":android-local"),
    project(":learning")
)

//publishing {
//    publications {
//        for (proj in projectList) {
//            create<MavenPublication>("maven-${proj.name}") {
//                groupId = "org.tenkiv.kuantify"
//                artifactId = "kuantify-${proj.name}"
//                version = project.version.toString()
//
//                from(components["java"])
//
//                for (file in proj.fileTree("build/libs").files) {
//                    when {
//                        file.name.contains("javadoc") -> {
//                            val a = artifact(file)
//                            a.classifier = "javadoc"
//                        }
//                        file.name.contains("sources") -> {
//                            val a = artifact(file)
//                            a.classifier = "sources"
//                        }
//                    }
//                }
//
//                if (proj == project(":android-local")) {
//                    for (file in proj.fileTree("build/outputs/aar").files) {
//                        if (file.name.contains("release")) {
//                            val a = artifact(file)
//                            a.classifier = null
//                        }
//                    }
//                }
//
//                pom {
//                    name.set(project.name)
//                    description.set(Info.pomDescription)
//                    url.set(System.getenv("CI_PROJECT_URL"))
//                    licenses {
//                        license {
//                            name.set(Info.pomLicense)
//                            url.set(Info.pomLicenseUrl)
//                        }
//                    }
//                    organization {
//                        name.set(Info.pomOrg)
//                    }
//                    scm {
//                        connection.set(System.getenv("CI_REPOSITORY_URL"))
//                        url.set(System.getenv("CI_PROJECT_URL"))
//                    }
//                }
//            }
//        }
//    }
//    repositories {
//        maven {
//            // change URLs to point to your repos, e.g. http://my.org/repo
//            val releasesRepoUrl = uri(Info.sonatypeReleaseRepoUrl)
//            val snapshotsRepoUrl = uri(Info.sonatypeSnapshotRepoUrl)
//            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
//            credentials {
//                username = System.getenv("MAVEN_REPO_USER")
//                password = System.getenv("MAVEN_REPO_PASSWORD")
//            }
//        }
//    }
//}

//signing {
//    sign(publishing.publications["mavenJava"])
//}