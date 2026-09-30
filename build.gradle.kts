import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
    // 1. Define your multiplatform native targets
    macosArm64()   // Apple Silicon macOS
    linuxX64()     // Linux on x86_64
    mingwX64()     // Windows x86_64

    // 2. Properly structure your source sets and block-scoped dependencies
    sourceSets {
        // commonMain holds your code and dependencies shared across Mac, Linux, and Windows
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:3.0.0")
            implementation("com.fleeksoft.ksoup:ksoup:0.2.5")
            implementation("com.fleeksoft.ksoup:ksoup-network:0.2.5")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    // 3. Configure binary outputs for all active native platforms
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries {
            sharedLib { // Generates a dynamic library (.so, .dylib, or .dll)
                baseName = "corrinenativelib" // Avoid empty strings here to prevent build errors
            }
        }
    }
}
