package dog.del.cli.features

import dog.del.cli.DogbinCli

class InitStoreFeature : CliFeature {
    override val metadata = FeatureMetadata(
        listOf("setstore"),
        listOf(
            arg(
                "location",
                help = "Location of the Store"
            ),
            arg(
                "environment",
                help = "Name of the Store"
            )
        ),
        mapOf(
            null to "Sets the store to be used for all future transactions."
        )
    )

    override fun execute(name: String, args: Map<String, String?>) {
        DogbinCli.Companion.Globals.storeLocation = args.getValue("location")!!
        DogbinCli.Companion.Globals.storeEnvironment = args.getValue("environment")!!
    }
}