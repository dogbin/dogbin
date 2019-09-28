val ktor_version: String by rootProject
val kotlin_version: String by rootProject
val logback_version: String by rootProject

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    compile("io.ktor:ktor-server-core:$ktor_version")

    compile("io.pebbletemplates:pebble:3.1.0")

    testCompile("io.ktor:ktor-server-tests:$ktor_version")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
