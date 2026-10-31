pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "hubidubicash-apps"
// -Phubidubicash.desktopOnly=true builds the desktop app without the Android
// SDK, as a Linux build container does; -Phubidubicash.androidOnly=true the
// phone app without the desktop one, as F-Droid would.
when {
    providers.gradleProperty("hubidubicash.desktopOnly").orNull == "true" -> include(":shared", ":desktop")
    providers.gradleProperty("hubidubicash.androidOnly").orNull == "true" -> include(":shared", ":android")
    else -> include(":shared", ":android", ":desktop")
}
