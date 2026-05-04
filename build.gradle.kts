plugins {
    kotlin("jvm")
}

allprojects {
    group = "org.sbm4j"
    version = "1.1.0"

    repositories {
        mavenCentral()
        maven {
            name = "githubPackages"
            url = uri("https://maven.pkg.github.com/sandrineBeauche/meercat")
            credentials {
                username = System.getenv("GITHUB_PACKAGE_REGISTRY_USER")
                password = System.getenv("GITHUB_PACKAGE_REGISTRY_TOKEN")
            }
        }
    }
}
dependencies {
    implementation(kotlin("stdlib-jdk8"))
}
repositories {
    mavenCentral()
}
kotlin {
    jvmToolchain(8)
}