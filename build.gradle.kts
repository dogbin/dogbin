import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    base
    kotlin("jvm") version "1.3.0"
}

allprojects {
    group = "dog.del"
    version = "2.0.0-SNAPSHOT"

    apply(plugin = "kotlin")

    repositories {
        mavenLocal()
        jcenter()
        maven { url = uri("https://kotlin.bintray.com/ktor") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://jitpack.io") }
    }

    tasks.withType<Test> {
        useJUnitPlatform {
            includeEngines("junit-jupiter")
        }

        failFast = true
        testLogging {
            events = setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
        }
    }
}