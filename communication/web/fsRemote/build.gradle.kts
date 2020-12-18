/*
 * Copyright 2020 Tenkiv, Inc.
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
 */

import java.util.*

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("org.jetbrains.dokka")
    signing
}

val properties = createPropertiesFromLocal()
setSigningExtrasFromProperties(properties)

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {
        all {
            languageSettings.useExperimentalAnnotation("kuantify.KuantifyComponentBuilder")
            languageSettings.useExperimentalAnnotation("kotlin.time.ExperimentalTime")
            languageSettings.useExperimentalAnnotation("org.tenkiv.coral.ExperimentalCoralApi")
        }

        val commonMain by getting {
            dependencies {
                api("org.tenkiv.kuantify:kuantify:$version")
                implementation("io.ktor:ktor-client-websockets:${Vof.ktor}")
                implementation("io.github.microutils:kotlin-logging:${Vof.kotlinLogging}")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }

    publishing {
        publications.withType<MavenPublication>().apply {
            val jvm by getting {
                artifactId = "kuantify-fs-websocket-remote-jvm"
            }
            val kotlinMultiplatform by getting {
                artifactId = "kuantify-fs-websocket-remote"
                artifact(tasks.getByName("metadataSourcesJar")) {
                    classifier = "sources"
                }
            }
            val metadata by getting {
                artifactId = "kuantify-fs-websocket-remote-metadata"
            }
        }.all {
            configureMavenPom(project)
            signing { if (project.isRelease) sign(this@all) }
        }

        setMavenRepositories()
    }
}