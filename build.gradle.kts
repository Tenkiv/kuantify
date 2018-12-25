@file:Suppress("KDocMissingDocumentation", "PublicApiImplicitType")

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
    base
    kotlin("jvm") version Versions.kotlin apply false
    kotlin("android") version Versions.kotlin apply false
    kotlin("android.extensions") version Versions.kotlin apply false
    id("kotlinx-serialization") version Versions.kotlin apply false

    //  id("com.android.library") version Versions.androidLibrary apply false
}

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:3.2.1")
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
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
        maven(url = "https://kotlin.bintray.com/ktor")
        maven(url = "https://kotlin.bintray.com/kotlinx")
    }
}

val isReleaseBuild get() = !version.toString().contains("SNAPSHOT")