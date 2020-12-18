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
plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("org.jetbrains.dokka")
    id("org.jetbrains.kotlin.plugin.serialization")
    signing
}

val properties = createPropertiesFromLocal()
setSigningExtrasFromProperties(properties)

kotlin {
    explicitApi()

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }

        mavenPublication {
            artifactId = "kuantify-jvm"
        }
    }

    sourceSets {
        all {
            languageSettings.enableLanguageFeature("InlineClasses")
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
            languageSettings.useExperimentalAnnotation("kotlinx.serialization.ExperimentalSerializationApi")
            languageSettings.useExperimentalAnnotation("kuantify.KuantifyComponentBuilder")
            languageSettings.useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
            languageSettings.useExperimentalAnnotation("kotlin.time.ExperimentalTime")
            languageSettings.useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.useExperimentalAnnotation("kotlinx.coroutines.FlowPreview")
            languageSettings.useExperimentalAnnotation("org.tenkiv.coral.ExperimentalCoralApi")
            languageSettings.useExperimentalAnnotation("io.ktor.util.InternalAPI")
        }

        val commonMain by getting {
            dependencies {
                //General kotlin utilities
                api("org.tenkiv.coral:coral:${Vof.coral}")
                implementation("org.jetbrains.kotlinx:atomicfu:${Vof.atomicfu}")

                //Coroutines
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Vof.coroutinesX}")

                //Serialization
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:${Vof.serializationX}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:${Vof.serializationX}")
                api("org.jetbrains.kotlinx:kotlinx-serialization-cbor:${Vof.serializationX}")

                //ktor
                implementation("io.ktor:ktor-network:${Vof.ktor}")

                //Logging
                implementation("io.github.microutils:kotlin-logging:${Vof.kotlinLogging}")

                //Units
                api("org.tenkiv.physikal:physikal:${Vof.physikal}")

                //Time
                api("org.jetbrains.kotlinx:kotlinx-datetime:${Vof.datetimeX}")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                //General kotlin utilities
                implementation(kotlin("reflect", Vof.kotlin))

                //Coroutines
                api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${Vof.coroutinesX}")
                api("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Vof.coroutinesX}")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("reflect", Vof.kotlin))
                implementation(kotlin("test", Vof.kotlin))
                implementation("io.mockk:mockk:${Vof.mockk}")
            }
        }
    }

    publishing {
        publications.withType<MavenPublication>().apply {
            val kotlinMultiplatform by getting {
                artifactId = "kuantify"
                artifact(tasks.getByName("metadataSourcesJar")) {
                    classifier = "sources"
                }
            }
            val metadata by getting {
                artifactId = "kuantify-metadata"
            }
        }.all {
            configureMavenPom(project)
            signing { if (isRelease) sign(this@all) }
        }

        setMavenRepositories()
    }
}