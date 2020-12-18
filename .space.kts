import circlet.pipelines.config.dsl.api.*
import java.io.*
import java.util.*

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

fun Job.jdk11Container(action: Container.() -> Unit) = container("openjdk:11") {
    action()
}

fun CompositeStep.jdk11Container(action: Container.() -> Unit) = container("openjdk:11") {
    action()
}

fun Container.gradleTask(task: String) = kotlinScript { api ->
    api.gradlew(task)
}

val gradleProperties: Properties
    get() {
        val properties = Properties()
        val propertiesFile = File("gradle.properties")
        if (propertiesFile.canRead()) {
            properties.load(FileInputStream(propertiesFile))
        }

        return properties
    }

val version = gradleProperties["version"].toString()
val isSnapshot = version.contains("SNAPSHOT")

job("Build, Test, Publish") {
    //============ core ============
    parallel {
        //assemble step
        jdk11Container {
            gradleTask(":core:assemble")
        }

        //test step
        jdk11Container {
            gradleTask(":core:allTests")
        }
    }

    //publish step
    container("openjdk:11") {
        kotlinScript { api ->
            println("Version is snapshot? $isSnapshot")
            if (isSnapshot) {
                println("Version is snapshot, publishing core library.")
                api.gradlew(":core:publish")
            }
        }
    }

    //============ communication ============
    //can build these are the same time, they are not dependent on the other
    parallel {
        //assemble steps
        jdk11Container {
            gradleTask(":communication:web:fsRemote:assemble")
            gradleTask(":communication:web:host:assemble")
        }

        //test steps
        jdk11Container {
            gradleTask(":communication:web:fsRemote:allTests")
            gradleTask(":communication:web:host:allTests")
        }
    }

    parallel {
        container("openjdk:11") {
            kotlinScript { api ->
                println("Version is snapshot? $isSnapshot")
                if (isSnapshot) {
                    println("Version is snapshot, publishing communication libraries.")
                    api.gradlew(":communication:web:fsRemote:publish")
                    api.gradlew(":communication:web:host:publish")
                }
            }
        }
    }
}