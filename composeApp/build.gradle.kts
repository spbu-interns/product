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
            }
        }
        val jsTest by getting {}
    }
}

repositories {
    mavenCentral()
}
