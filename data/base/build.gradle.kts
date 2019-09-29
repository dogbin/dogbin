val kotlin_version: String by rootProject

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")

    compile("org.jetbrains.xodus:dnq:1.3.440")
    compile("at.favre.lib:bcrypt:0.8.0")

    compile(project(":data:model"))
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")