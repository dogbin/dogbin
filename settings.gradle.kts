pluginManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://kotlin.bintray.com/kotlin-eap") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}

rootProject.name = "dogbin"

include("app", "commons", "data", "data:model", "data:migration", "data:base", "cli")