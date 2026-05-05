plugins {
    id("ktscraping.common-conventions")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
    `java-test-fixtures`
}

version = "1.1-SNAPSHOT"


val kodeinVersion: String by project
val ktorVersion: String by project
val meercatVersion: String by project
val mockkVersion: String by project
val hamkrestVersion: String by project

repositories {
    maven {
        name = "githubPackages"
        url = uri("https://maven.pkg.github.com/sandrineBeauche/meercat")
        credentials {
            username = System.getenv("GITHUB_PACKAGE_REGISTRY_USER")
            password = System.getenv("GITHUB_PACKAGE_REGISTRY_TOKEN")
        }
    }
}


dependencies {
    implementation(kotlin("reflect"))


    api ("org.kodein.di:kodein-di:$kodeinVersion")
    api("org.kodein.di:kodein-di-jvm:$kodeinVersion")


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

    api("org.sbm4j:meercat:${meercatVersion}")
    testImplementation(testFixtures("org.sbm4j:meercat:${meercatVersion}"))
    testImplementation("org.sbm4j:meercat:${meercatVersion}:test-fixtures-sources")
    testFixturesApi(testFixtures("org.sbm4j:meercat:${meercatVersion}"))
    testFixturesApi("org.sbm4j:meercat:${meercatVersion}:test-fixtures-sources")
    testImplementation(testFixtures(project(":ktscraping-core")))
    testFixturesImplementation("io.mockk:mockk:${mockkVersion}")
    testFixturesImplementation("com.natpryce:hamkrest:${hamkrestVersion}")

    testImplementation("org.sbm4j:meercat:${meercatVersion}")

    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
    testFixturesImplementation("io.github.oshai:kotlin-logging-jvm:5.1.0")
}