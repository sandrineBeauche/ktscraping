plugins {
    id("ktscraping.common-conventions")
}

version = "1.1-SNAPSHOT"

val playwrightVersion: String by project

dependencies {
    implementation(project(":ktscraping-core"))

    testImplementation(testFixtures(project(":ktscraping-core")))
    api("com.microsoft.playwright:playwright:${playwrightVersion}")
}

tasks.register<JavaExec>("codegen") {
    mainClass = "com.microsoft.playwright.CLI"
    classpath = sourceSets["main"].runtimeClasspath
    args = mutableListOf("codegen")
}