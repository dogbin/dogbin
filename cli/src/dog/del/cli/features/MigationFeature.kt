package dog.del.cli.features

import com.mongodb.MongoCredential
import dog.del.cli.DogbinCli
import dog.del.data.migration.MongoMigration

class MigationFeature : CliFeature {
    override val metadata = FeatureMetadata(
        listOf("migrate"),
        listOf(
            arg(
                "host",
                false,
                "The host on which mongodb is running. Defaults to localhost."
            ),
            arg(
                "db",
                false,
                "The name of the db. Defaults to dogbin_dev."
            ),
            arg(
                "username",
                false
            ),
            arg(
                "password",
                false
            )
        ),
        mapOf(
            null to "Migrate from mongodb to the new Xodus backend"
        )
    )

    override fun execute(name: String, args: Map<String, String?>) {
        val host = args["host"] ?: "localhost"
        val dbName = args["db"] ?: "dogbin_dev"
        val username = args["username"]
        val password = args["password"]
        val credential = if (username != null && password != null) MongoCredential.createCredential(username, dbName, password.toCharArray()) else null

        MongoMigration(
            xdStore = DogbinCli.Companion.Globals.getStore(),
            mongoHost = host,
            dbName = dbName,
            credential = credential
        ).migrate()
    }
}