plugins {
    kotlin("jvm") version "2.3.0"
    `maven-publish`
}

group = "com.github.Corrinedev"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jsoup:jsoup:1.17.2")
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.named<Test>("test") {
    failOnNoDiscoveredTests = false
}