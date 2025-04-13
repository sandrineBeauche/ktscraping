plugins {
    kotlin("jvm") version "2.0.20"
    application
    kotlin("plugin.serialization") version "1.8.0"
    id("com.vanniktech.maven.publish") version "0.31.0-rc2"
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
val ktorVersion: String by project

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:$coroutinesVersion")

    implementation(kotlin("reflect"))

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
    api ("org.kodein.di:kodein-di:$kodeinVersion")
    api("org.kodein.di:kodein-di-jvm:$kodeinVersion")
    api("com.microsoft.playwright:playwright:$playwrightVersion")

    api("it.skrape:skrapeit:1.2.2")
    implementation("com.fleeksoft.ksoup:ksoup:0.2.1")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    implementation("org.dizitart:nitrite-bom:4.3.0")
    implementation("org.dizitart:nitrite:4.3.0")
    implementation("org.dizitart:potassium-nitrite:4.3.0")
    implementation("org.dizitart:nitrite-mvstore-adapter:4.3.0")

    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.1")
    implementation("com.nfeld.jsonpathkt:jsonpathkt:2.0.1")

    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation(kotlin("test"))
    testImplementation("com.natpryce:hamkrest:$hamkrestVersion")


    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
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

publishing{
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/sandrineBeauche/ktscraping")
            // username and password (a personal Github access token) should be specified as
            // `githubPackagesUsername` and `githubPackagesPassword` Gradle properties or alternatively
            // as `ORG_GRADLE_PROJECT_githubPackagesUsername` and `ORG_GRADLE_PROJECT_githubPackagesPassword`
            // environment variables
            credentials(PasswordCredentials::class)
        }
    }
}