val kotlin_version: String by rootProject

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    compile(project(":commons"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")