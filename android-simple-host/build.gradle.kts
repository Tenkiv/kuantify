plugins {
    id("com.android.application")
    kotlin("android")
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
        multiDexEnabled = true
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

    packagingOptions {
        pickFirst("META-INF/**")
        pickFirst("tec/units/indriya/format/messages.properties")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation(project(":android-local"))
    implementation(group = "io.ktor", name = "ktor-server-netty", version = Vof.ktor)

    testImplementation(group = "junit", name = "junit", version = Vof.junit)
}