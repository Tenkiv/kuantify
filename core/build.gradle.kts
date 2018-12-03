import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright 2018 Tenkiv, Inc.
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
    kotlin("jvm")
    id("kotlinx-serialization")
}

apply(from = "../maven_push.gradle")

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    // Networking
    compile(group = "io.ktor", name = "ktor-network", version = Versions.ktor)
    compile(group = "io.ktor", name = "ktor-network-tls", version = Versions.ktor)

    //Serialization
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = Versions.jackson)
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = Versions.jackson)

    //Units of measurement
    compile(group = "org.tenkiv.physikal", name = "complete-units", version = "2.0.6.0-SNAPSHOT")

    //General kotlin utilities
    compile(group = "org.tenkiv.coral", name = "coral", version = "2.1.0.1-SNAPSHOT")
    compile(group = "io.arrow-kt", name = "arrow-core", version = "0.7.2")
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-reflect")

    //Coroutines
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = Versions.coroutinesX)


    //Test dependencies
    testImplementation(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-serialization-runtime",
        version = Versions.serializationX
    )
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"

    kotlinOptions.freeCompilerArgs += "-XXLanguage:+InlineClasses"
}
