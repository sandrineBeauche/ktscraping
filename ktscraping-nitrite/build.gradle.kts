plugins {
    id("ktscraping.common-conventions")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
}

version = "1.1-SNAPSHOT"

val nitriteVersion: String by project

dependencies {
    implementation(project(":ktscraping-core"))

    implementation("org.dizitart:nitrite-bom:$nitriteVersion")
    implementation("org.dizitart:nitrite:$nitriteVersion")
    implementation("org.dizitart:potassium-nitrite:$nitriteVersion")
    implementation("org.dizitart:nitrite-mvstore-adapter:$nitriteVersion")

    testImplementation("io.github.serpro69:kotlin-faker:1.16.1")
}