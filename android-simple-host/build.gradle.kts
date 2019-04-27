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

plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(28)
    
    defaultConfig {
        applicationId = "org.tenkiv.kuantify.simple_host"

        minSdkVersion(26)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }

    dataBinding {
        setEnabled(true)
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

    packagingOptions {
        pickFirst("META-INF/**")
        pickFirst("tec/units/indriya/format/messages.properties")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(project(":android-local"))
    implementation(group = "com.android.support", name = "support-compat", version = "28.0.0")
    implementation(group = "com.android.support", name = "localbroadcastmanager", version = "28.0.0")
    implementation(group = "androidx.constraintlayout", name = "constraintlayout", version = "1.1.3")
    implementation(group = "io.ktor", name = "ktor-server-netty", version = Vof.ktor)
    implementation(group ="com.noveogroup.android", name = "android-logger", version = "1.3.1")
    implementation(group = "io.github.microutils", name = "kotlin-logging", version = "1.6.24")
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version = Vof.coroutinesX)
    implementation(group = "androidx.databinding", name = "databinding-common", version = "3.4.0")
}