val kotlin_version: String by rootProject

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")

    testCompile("org.junit.jupiter:junit-jupiter:5.6.0-M1")
    testCompile("com.natpryce:hamkrest:1.7.0.0")
}

kotlin.sourceSets["main"].kotlin.srcDirs("src")
kotlin.sourceSets["test"].kotlin.srcDirs("test")