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

    //General kotlin utilities
    compile(group = "org.tenkiv.coral", name = "coral", version = Vof.coral)
    compile(group = "io.arrow-kt", name = "arrow-core", version = Vof.arrow)
    implementation(group = "org.jetbrains.kotlin", name = "kotlin-reflect")

    //Coroutines
    compile(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = Vof.coroutinesX)

    //Logging
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = Vof.logging)

    //Serialization
    implementation(
        group = "org.jetbrains.kotlinx",
        name = "kotlinx-serialization-runtime",
        version = Vof.serializationX
    )
    implementation(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = Vof.jackson)
    implementation(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = Vof.jackson)

    //Units of measurement
    compile(group = "org.tenkiv.physikal", name = "complete-units", version = Vof.physikal)

    //ktor
    implementation(group = "io.ktor", name = "ktor-server-netty", version = Vof.ktor)
    implementation(group = "io.ktor", name = "ktor-websockets", version = Vof.ktor)

    implementation(group = "io.ktor", name = "ktor-client-core", version = Vof.ktor)
    implementation(group = "io.ktor", name = "ktor-client-websocket", version = Vof.ktor)


    //Test
    testImplementation(group = "org.slf4j", name = "slf4j-simple", version = Vof.slf4j)
    testImplementation(group = "io.ktor", name = "ktor-client-cio", version = Vof.ktor)

}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
