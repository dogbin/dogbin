val kotlin_version: String by rootProject

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.3.2")

    compile("org.jetbrains.xodus:dnq:1.3.440")
    compile("com.amdelamar:jhash:2.1.0")

    compile(project(":data:model"))
    compile(project(":commons"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")