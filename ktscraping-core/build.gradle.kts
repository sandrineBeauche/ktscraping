plugins {
    id("ktscraping.common-conventions")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
}


val logbackVersion: String by project
val coroutinesVersion: String by project
val mockkVersion: String by project
val kodeinVersion: String by project
val playwrightVersion: String by project
val hamkrestVersion: String by project
val kotlinLoggingVersion: String by project
val ktorVersion: String by project
val meercatVersion: String by project

dependencies {
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

    implementation("org.sbm4j:meercat:${meercatVersion}")
    testImplementation(testFixtures("org.sbm4j:meercat:${meercatVersion}"))
    testImplementation("org.sbm4j:meercat:${meercatVersion}:test-fixtures-sources")

    testImplementation("io.mockk:mockk:${mockkVersion}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation(kotlin("test"))
    testImplementation("com.natpryce:hamkrest:$hamkrestVersion")
    testImplementation("org.sbm4j:meercat:1.1.1")


    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
}

tasks.test {
    useJUnitPlatform()
}


tasks.register<JavaExec>("codegen") {
    mainClass = "com.microsoft.playwright.CLI"
    classpath = sourceSets["main"].runtimeClasspath
    args = mutableListOf("codegen")
}


