pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "InternetRadio"
include(":app")
include(":wear") 