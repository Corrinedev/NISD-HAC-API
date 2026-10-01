import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeBinaryTestRun

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.4.20"
    `maven-publish`
}

group = "com.github.Corrinedev"
version = "2.1.0-native"

repositories {
    mavenCentral()
}

kotlin {
    //Desktop
    macosArm64()   // Apple Silicon macOS
    linuxX64()     // Linux on x86_64
    mingwX64()     // Windows x86_64

    //Mobile
    //androidTarget {
    //    publishLibraryVariants("release") //AAR
    //}
    iosArm64()          // Physical iOS devices
    iosSimulatorArm64() // Mac M1/M2/M3 simulators

    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:3.0.0")
            implementation("com.fleeksoft.ksoup:ksoup-network:0.2.5") {
                exclude(group = "com.fleeksoft.io", module = "io")
            }
            // Force the app to use the newer 'io-core' module
            implementation("com.fleeksoft.io:io-core:0.0.4")
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        // ADD THIS: Engine for Apple Silicon Mac Execution
        val macosArm64Main by getting {
            dependencies {
                implementation("io.ktor:ktor-client-darwin:3.0.0")
            }
        }

        // ADD THIS: Engine for Linux Execution
        val linuxX64Main by getting {
            dependencies {
                implementation("io.ktor:ktor-client-curl:3.0.0")
            }
        }

        // ADD THIS: Engine for Windows Execution
        val mingwX64Main by getting {
            dependencies {
                implementation("io.ktor:ktor-client-winhttp:3.0.0")
            }
        }
    }

    targets.withType<KotlinNativeTarget>().configureEach {
        compilations.configureEach {
        }

        binaries {
            // Keep your shared library setup intact
            sharedLib {
                baseName = "corrinenativelib"
            }

            // ADD THIS: Generates a runnable native application for testing
            executable {
                baseName = "testApp"
                entryPoint = "main" // Tells it to look for a standard fun main()
            }
        }
    }
}

publishing {
    publications {
        // Gradle automatically creates a "kotlinMultiplatform" publication under the hood
        // that links the Android AAR and iOS KLIBs together.
        withType<MavenPublication> {
            // Optional customized naming info:
            // groupId = "com.yourname"
            // artifactId = "mylibrary"
            // version = "1.0.0"
        }
    }

    // 4. Point to your repository (e.g., GitHub Packages, Maven Central, or Local)
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://github.com")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}