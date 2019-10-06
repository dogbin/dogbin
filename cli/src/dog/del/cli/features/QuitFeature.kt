package dog.del.cli.features

import kotlin.system.exitProcess

class QuitFeature : CliFeature {
    override val metadata = FeatureMetadata(
        listOf("quit", "exit", "bye"),
        listOf(),
        mapOf(
            null to "Exits the management shell"
        )
    )

    override fun execute(name: String, args: Map<String, String?>) {
        exitProcess(0)
    }
}