import java.util.*

plugins {
    kotlin("multiplatform")
    id("maven-publish")
    id("org.jetbrains.dokka")
    signing
}

val properties: Properties = createPropertiesFromLocal()
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

        tasks {
            register<Jar>("javadocJar") {
                archiveClassifier.set("javadoc")
                from(dokkaJavadoc)
            }
        }
    }

    publishing {
        publications.withType<MavenPublication>().apply {
            println(this.names)
            val jvm by getting {
                artifactId = "kuantify-fs-websocket-remote-jvm"
                artifact(tasks.getByName("javadocJar"))
            }
            val kotlinMultiplatform by getting {
                artifactId = "kuantify-fs-websocket-remote"
            }
            val metadata by getting {
                artifactId = "kuantify-fs-websocket-remote-metadata"
            }
        }.forEach {
            it.configureMavenPom(project.isRelease, project)
            signing { if (project.isRelease) sign(it) }
        }

        setMavenRepositories(project.isRelease, properties)
    }
}