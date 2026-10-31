import com.android.build.api.artifact.SingleArtifact

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
}

android {
    namespace = "com.hubidubicash.mobile"
    // Play takes new apps only at its current target level.
    compileSdk = 36

    defaultConfig {
        // Play knows the app by this id. -Phubidubicash.appId=… installs a
        // build under another id, which is a second, empty app beside it.
        applicationId = (project.findProperty("hubidubicash.appId") as String?)
            ?: "com.saferendering.hubidubicash"
        minSdk = 26
        targetSdk = 36
        versionCode = (project.property("hubidubicash.versionCode") as String).toInt()
        versionName = project.property("hubidubicash.version") as String
    }

    // A release build is signed only where a key is set up, and never from the
    // repo: HUBIDUBICASH_KEYSTORE (with _PASSWORD and _ALIAS) in the
    // environment, or hubidubicash.keystore, .keystoreAlias and
    // .keystoreKeychain in ~/.gradle/gradle.properties, the password then read
    // from that macOS Keychain item. Without one the release build comes out
    // unsigned, the way F-Droid builds it.
    signingConfigs {
        create("release") {
            val path = System.getenv("HUBIDUBICASH_KEYSTORE")
                ?: project.findProperty("hubidubicash.keystore") as String?
            if (path != null && file(path).exists()) {
                val keychain = project.findProperty("hubidubicash.keystoreKeychain") as String?
                val password = System.getenv("HUBIDUBICASH_KEYSTORE_PASSWORD")
                    ?: keychain?.let { item ->
                        providers.exec {
                            commandLine("security", "find-generic-password", "-s", item, "-w")
                            isIgnoreExitValue = true
                        }.standardOutput.asText.get().trim()
                    }
                    ?: error("No password for $path: set HUBIDUBICASH_KEYSTORE_PASSWORD or hubidubicash.keystoreKeychain")
                storeFile = file(path)
                storePassword = password
                keyAlias = System.getenv("HUBIDUBICASH_KEYSTORE_ALIAS")
                    ?: project.findProperty("hubidubicash.keystoreAlias") as String? ?: "upload"
                keyPassword = password
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.getByName("release").takeIf { it.storeFile != null }?.let { signingConfig = it }
            // R8 drops unused code and shortens names: a smaller app that starts faster.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // Google's encrypted list of the libraries inside stays out of the APK;
    // Play still gets it in the bundle.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}

// The app never goes online: no permission in the finished manifest may let
// it, whichever library asked for one. `check` fails the build if one does.
val networkPermissions = listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.ACCESS_WIFI_STATE", "android.permission.CHANGE_WIFI_STATE")
androidComponents {
    onVariants { variant ->
        val manifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val task = tasks.register("check${name}Permissions") {
            description = "Fails if the ${variant.name} app could reach the network."
            group = "verification"
            inputs.file(manifest)
            doLast {
                val text = manifest.get().asFile.readText()
                val found = networkPermissions.filter { "\"$it\"" in text }
                check(found.isEmpty()) { "The ${variant.name} app asks for $found; HubidubiCash never goes online." }
            }
        }
        tasks.matching { it.name == "check" }.configureEach { dependsOn(task) }
    }
}
