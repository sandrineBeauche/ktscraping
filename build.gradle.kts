plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "org.sbm4j"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


val kotlin_version: String by project
val logback_version: String by project
val coroutines_version: String by project
val mockk_version: String by project
val kodein_version = "7.19.0"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutines_version")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutines_version")

    implementation(kotlin("reflect"))

    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.kodein.di:kodein-di:$kodein_version")
    implementation("org.kodein.di:kodein-di-jvm:$kodein_version")

    testImplementation("io.mockk:mockk:${mockk_version}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines_version")
    testImplementation(kotlin("test"))
    testImplementation("com.natpryce:hamkrest:1.8.0.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}