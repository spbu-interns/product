plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                cssSupport { enabled.set(true) }
                outputFileName = "composeApp.js"
            }
            binaries.executable()
        }
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("io.kvision:kvision:9.1.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("io.ktor:ktor-client-core:3.2.3")
                implementation("io.ktor:ktor-client-js:3.2.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.2.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")
            }
        }
        val jsTest by getting {}
    }
}

repositories {
    mavenCentral()
}
