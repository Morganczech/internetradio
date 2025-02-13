buildscript {
    extra.apply {
        set("compose_version", "1.5.4")
        set("kotlin_version", "1.9.20")
        set("agp_version", "7.3.1")
    }
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://maven.google.com") }
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.8.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.extra["kotlin_version"]}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.50")
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.9.20-1.0.14")
    }
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf(
                "-Xjvm-default=all",
                "-Xopt-in=kotlin.RequiresOptIn"
            )
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
} 