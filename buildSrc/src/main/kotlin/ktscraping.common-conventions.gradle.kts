plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
    id("java-test-fixtures")
    idea
}

group = "org.sbm4j.ktscraping"

val logbackVersion: String by project
val coroutinesVersion: String by project
val mockkVersion: String by project
val hamkrestVersion: String by project
val kotlinLoggingVersion: String by project

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-debug:${coroutinesVersion}")
    implementation("ch.qos.logback:logback-classic:${logbackVersion}")
    implementation("io.github.oshai:kotlin-logging-jvm:5.1.0")

    testImplementation(kotlin("test"))

    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("com.natpryce:hamkrest:${hamkrestVersion}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${coroutinesVersion}")
}

kotlin {
    jvmToolchain(21)
}


publishing {
    repositories {
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/sandrineBeauche/ktscraping")
            credentials {
                username = System.getenv("GITHUB_PACKAGE_REGISTRY_USER")
                password = System.getenv("GITHUB_PACKAGE_REGISTRY_TOKEN")
            }
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

tasks.test {
    useJUnitPlatform()
}