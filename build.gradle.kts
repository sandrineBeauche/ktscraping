plugins {
    kotlin("jvm") version "2.0.20"
    application
    kotlin("plugin.serialization") version "1.8.0"
}

group = "org.sbm4j"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


val kotlinVersion: String by project
val logbackVersion: String by project
val coroutinesVersion: String by project
val mockkVersion: String by project
val kodeinVersion: String by project
val playwrightVersion: String by project
val hamkrestVersion: String by project
val kotlinLoggingVersion: String by project

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")

    implementation(kotlin("reflect"))

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    implementation("org.kodein.di:kodein-di:$kodeinVersion")
    implementation("org.kodein.di:kodein-di-jvm:$kodeinVersion")
    implementation("com.microsoft.playwright:playwright:$playwrightVersion")

    implementation("it.skrape:skrapeit:1.2.2")
    implementation("com.fleeksoft.ksoup:ksoup:0.2.1")

    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation(kotlin("test"))
    testImplementation("com.natpryce:hamkrest:$hamkrestVersion")


    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21
    )
}

application {
    mainClass.set("MainKt")
}

task("codegen", JavaExec::class) {
    mainClass = "com.microsoft.playwright.CLI"
    classpath = sourceSets["main"].runtimeClasspath
    args = mutableListOf("codegen")
}