// Everything both apps share, written once: the model and its maths, the
// translations, the database and every screen. Both targets are JVMs, so the
// common code uses java.time, java.text and javax.crypto directly.
plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

val desktopOnly = providers.gradleProperty("hubidubicash.desktopOnly").orNull == "true"
val androidOnly = providers.gradleProperty("hubidubicash.androidOnly").orNull == "true"
if (!desktopOnly) apply(plugin = "com.android.library")

kotlin {
    jvmToolchain(17)
    // Room writes the database's constructor as an actual object for each platform.
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
    if (!desktopOnly) androidTarget()
    if (!androidOnly) jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.materialIconsExtended)
            api(compose.ui)
            api("androidx.room:room-runtime:2.7.2")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
        }
        if (!desktopOnly) {
            androidMain.dependencies {
                api("androidx.activity:activity-compose:1.9.2")
            }
        }
        if (!androidOnly) {
            val desktopMain by getting {
                dependencies {
                    api(compose.desktop.currentOs)
                    api("androidx.sqlite:sqlite-bundled:2.5.2")
                    api("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
                }
            }
            val desktopTest by getting {
                dependencies {
                    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                    implementation(compose.uiTest)
                }
            }
        }
    }
}

if (!desktopOnly) {
    extensions.configure<com.android.build.gradle.LibraryExtension> {
        namespace = "com.hubidubicash.shared"
        compileSdk = 36
        defaultConfig { minSdk = 26 }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
}

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    if (!desktopOnly) add("kspAndroid", "androidx.room:room-compiler:2.7.2")
    if (!androidOnly) add("kspDesktop", "androidx.room:room-compiler:2.7.2")
}

if (!androidOnly) {
    tasks.named<Test>("desktopTest") {
        useJUnitPlatform()
        // The UI tests draw offscreen.
        systemProperty("java.awt.headless", "true")
    }
}
