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
                implementation(project(":shared"))

                implementation("io.kvision:kvision:9.1.1")
                implementation("io.kvision:kvision-toastify:9.1.1")
                implementation("io.kvision:kvision-i18n:9.1.1")

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-js:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-client-logging:2.3.12")

                implementation("io.kvision:kvision-datetime:9.1.1")
            }
        }
        val jsTest by getting {}
    }
}

repositories {
    mavenCentral()
}