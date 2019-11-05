val kotlin_version: String by rootProject

plugins {
    kotlin("kapt")
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")

    compile("org.litote.kmongo:kmongo:3.11.1")
    compile("me.tongfei:progressbar:0.7.4")
    kapt("org.litote.kmongo:kmongo-annotation-processor:3.11.0")

    // Disable logging from xodus
    compile("org.slf4j:slf4j-nop:1.7.28")

    compile(project(":data:base"))
    compile(project(":commons"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")