val ktor_version: String by rootProject
val kotlin_version: String by rootProject
val logback_version: String by rootProject
val koin_version: String by rootProject

plugins {
    application
    kotlin("jvm")
    id("com.google.cloud.tools.jib") version "1.7.0"
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    compile("io.ktor:ktor-server-netty:$ktor_version")
    compile("ch.qos.logback:logback-classic:$logback_version")
    compile("io.ktor:ktor-metrics:$ktor_version")
    compile("io.ktor:ktor-server-core:$ktor_version")
    compile("io.ktor:ktor-gson:$ktor_version")
    compile(project(":pebble"))

    compile("com.github.zensum:ktor-health-check:011a5a8")
    compile("org.koin:koin-ktor:$koin_version")
    compile(project(":data:base"))
    compile(project(":commons"))

    testCompile("io.ktor:ktor-server-tests:$ktor_version")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")

sourceSets["main"].resources.srcDirs("resources")
sourceSets["test"].resources.srcDirs("testresources")
