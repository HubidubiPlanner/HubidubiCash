import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.hubidubicash.desktop.MainKt"
        // The version a backup says it was made by.
        jvmArgs += "-Dhubidubicash.version=${project.property("hubidubicash.version")}"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "HubidubiCash"
            packageVersion = project.property("hubidubicash.version") as String
            description = "HubidubiCash: see where your money goes, and keep more of it"
            vendor = "HubidubiPlanner"
            licenseFile.set(rootProject.file("../../LICENSE"))
            // java.sql for SQLite; jdk.localedata so Polish dates and "zł"
            // come out right in the packaged app, not only in a development run.
            modules("java.sql", "jdk.localedata", "jdk.unsupported")
            linux {
                iconFile.set(project.file("icon.png"))
                packageName = "hubidubicash"
                appCategory = "Office"
                menuGroup = "Office"
                shortcut = true
            }
            macOS {
                bundleID = "com.hubidubicash.desktop"
                iconFile.set(project.file("icon.icns"))
            }
        }
    }
}

tasks.test { useJUnitPlatform() }
