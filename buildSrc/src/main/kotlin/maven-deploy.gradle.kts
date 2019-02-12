plugins {
    maven
    signing
}

tasks {
    register("hello") {
        doLast {
            println("hello")
        }
    }
}