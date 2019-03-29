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
    java
    kotlin("jvm") version Vof.kotlin apply false
    kotlin("android") version Vof.kotlin apply false
    kotlin("android.extensions") version Vof.kotlin apply false
    id("kotlinx-serialization") version Vof.kotlin apply false
    id("org.jetbrains.dokka") version Vof.dokka apply false
    `maven-publish`
}

buildscript {
    repositories {
        google()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:${Vof.androidGradle}")
    }
}

val isRelease = !version.toString().endsWith("SNAPSHOT")
val nonAndroidProjects = subprojects.filter {
    it.name == "core" || it.name == "learning" || it.name == "android-core"
}

subprojects {
    repositories {
        mavenCentral()
        jcenter()
        google()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://kotlin.bintray.com/ktor")
        maven(url = "https://kotlin.bintray.com/kotlinx")
    }
}

println(nonAndroidProjects)

configure(nonAndroidProjects) {
    apply<MavenPublishPlugin>()
    apply<JavaPlugin>()

    tasks {
        register<Jar>("sourcesJar") {
            from(sourceSets.main.get().allSource)
            archiveClassifier.set("sources")
        }

        register<Jar>("javadocJar") {
            from(getByName("dokka"))
            archiveClassifier.set("javadoc")
        }

        getByName("build") {
            dependsOn("sourcesJar")
            dependsOn("javadocJar")
        }
    }
}
