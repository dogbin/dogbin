val kotlin_version: String by rootProject

plugins {
    application
    kotlin("jvm")
}

application {
    mainClassName = "dog.del.cli.DogbinCli"
    applicationDefaultJvmArgs = listOf("-Xmx6g")
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")

    compile(project(":data:base"))
    compile(project(":data:migration"))
    compile(project(":commons"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")