package dog.del.cli.features

import java.nio.file.Files
import java.nio.file.Paths

class EnvironmentHelperFeature : CliFeature {
    override val metadata = FeatureMetadata(
        listOf("pwd", "ls", "cd"),
        listOf(
            arg(
                "path",
                false,
                "Directory to list the files inside. Default is the current working directory.",
                listOf("ls")
            ),
            arg(
                "path",
                true,
                "Directory to change to.",
                listOf("cd")
            )
        ),
        mapOf(
            "pwd" to "Prints the current working directory",
            "ls" to "Lists files inside a directory",
            "cd" to "Changes the working directory"
        )
    )

    override fun execute(name: String, args: Map<String, String?>) {
        val basePath = System.getProperty("user.dir")
        val path = args["path"]
        when(name) {
            "pwd" -> println(Paths.get(basePath).toAbsolutePath().normalize().toString())
            "ls" -> Files.list(Paths.get(path ?: basePath)).forEach {
                print(it.fileName)
                if (Files.isDirectory(it)) {
                    print("/")
                }
                println()
            }
            "cd" -> System.setProperty("user.dir", Paths.get(basePath, path!!).toAbsolutePath().normalize().toString())
        }
    }
}