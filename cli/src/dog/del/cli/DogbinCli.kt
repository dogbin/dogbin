package dog.del.cli

import dog.del.cli.features.FeatureManager
import dog.del.data.base.Database
import jetbrains.exodus.database.TransientEntityStore
import java.io.File

class DogbinCli {
    private val featureManager = FeatureManager()

    fun repl() {
        println("Welcome to the dogbin server management shell!")
        println("Type \"help\" to view the help")
        while (true) {
            print("${System.getProperty("user.dir")} > ")
            val input = readLine() ?: continue
            try {
                featureManager.evaluate(input)
            } catch (e: Exception) {
                println("Error: ${e.message}")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            DogbinCli().repl()
        }

        object Globals {
            private var store: TransientEntityStore? = null
            private var storeEnvHash: Int = 0
            private val currentStoreEnvHash get() = (storeLocation + storeEnvironment).hashCode()

            var storeLocation: String = ""
            var storeEnvironment: String = ""

            fun getStore(): TransientEntityStore {
                if (storeLocation.isNotBlank() && storeEnvironment.isNotBlank()) {
                    if (storeEnvHash != currentStoreEnvHash) {
                        store = Database.init(File(System.getProperty("user.dir"), storeLocation), storeEnvironment)
                        storeEnvHash = currentStoreEnvHash
                    }
                    return store!!
                }
                throw IllegalStateException("Please set a store before using a command requiring one")
            }
        }
    }
}