plugins {
    alias(libs.plugins.kotlinMultiplatform)
    kotlin("plugin.serialization") version "2.2.10"
}

kotlin {
    jvm() {
    }

    js(IR) {
        browser {
            binaries.executable()
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

    }

    tasks.withType<Test>().configureEach {
        enabled = false
    }

    tasks.matching { it.name.contains("jsTest") || it.name.contains("jsBrowserTest") }.configureEach {
        enabled = false
    }

}