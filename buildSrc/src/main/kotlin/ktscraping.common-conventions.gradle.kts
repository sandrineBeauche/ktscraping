plugins {
    id("org.jetbrains.kotlin.jvm")
    id("maven-publish")
    id("java-test-fixtures")
}

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