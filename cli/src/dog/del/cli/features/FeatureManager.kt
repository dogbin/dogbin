package dog.del.cli.features

class FeatureManager {
    private val features = listOf<CliFeature>(
        HelpFeature(),
        QuitFeature(),
        EnvironmentHelperFeature(),
        InitStoreFeature(),
        UserFeature(),
        DocumentFeature(),
        MigationFeature()
    )

    fun evaluate(command: String) {
        val parts = command.trim().split(' ').map { it.trim() }
        val name = parts[0]
        val args = parts.subList(1, parts.size)
        val feature = features.firstOrNull { name in it.metadata.names } ?: error("No feature named $name exists")
        val metadata = feature.metadata
        if (args.size in metadata.argsFor(name, true).size..metadata.argsFor(name).size) {
            feature.execute(name, args.mapIndexed { i, arg ->
                metadata.argsFor(name)[i].name to arg
            }.toMap())
        } else {
            error("Wrong amount of arguments supplied")
        }
    }

    inner class HelpFeature: CliFeature {
        override val metadata = FeatureMetadata(
            listOf("help", "h", "?"),
            listOf(
                arg(
                    "command",
                    false,
                    "Command to show detailed help for"
                )
            ),
            mapOf(null to "Lists available features and how to use them")
        )

        override fun execute(name: String, args: Map<String, String?>) {
            val command = args["command"]
            if (command == null) {
                features.map { it.metadata }.forEach {
                    it.names.forEachIndexed { index, name ->
                        print(name)
                        if (it.help.isNotEmpty() || index > 0) {
                            print(" - ")
                            val help = it.help[name]
                            println(if (help == null && index > 0)
                                "(alias of ${it.names[0]}) " + (it.help[null] ?: "")
                            else help ?: it.help[null])
                        }
                    }
                }
            } else {
                val feature = features.firstOrNull { command in it.metadata.names } ?: error("No feature named $command exists")
                val metadata = feature.metadata
                val index = metadata.names.indexOf(command)
                if (metadata.help.isNotEmpty() || index > 0) {
                    val help = metadata.help[command]
                    println(if (help == null && index > 0)
                        "(alias of ${metadata.names[0]}) " + (metadata.help[null] ?: "")
                    else help ?: metadata.help[null])
                }
                print("Usage: $command ")
                println(metadata.argsFor(command).joinToString(" ") { if (it.required) it.name else "[${it.name}]" })
                for (arg in metadata.argsFor(command)) {
                    print("- ${arg.name}")
                    if (!arg.help.isNullOrBlank()) {
                        print(": ${arg.help}")
                    }
                    println()
                }
            }
        }
    }
}