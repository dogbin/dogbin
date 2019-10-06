package dog.del.cli.features

interface CliFeature {
    val metadata: FeatureMetadata
    fun execute(name: String, args: Map<String, String?>)
}

data class FeatureMetadata(
    val names: List<String>,
    val args: List<ArgData>,
    val help: Map<String?, String> = mapOf()
) {
    fun argsFor(name: String, requiredOnly: Boolean = false) = args.filter { (!requiredOnly || it.required) && (it.forNames.isEmpty() || name in it.forNames) }
}

data class ArgData(
    val name: String,
    val required: Boolean,
    val help: String?,
    val forNames: List<String>
)

fun arg(name: String, required: Boolean = true, help: String? = null, commands: List<String> = listOf()) = ArgData(name, required, help, commands)
