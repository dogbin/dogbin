package dog.del.cli.features

import dog.del.cli.DogbinCli
import dog.del.commons.format
import dog.del.data.base.model.user.XdUser
import dog.del.data.base.model.user.XdUserRole

class UserFeature : CliFeature {
    override val metadata = FeatureMetadata(
        listOf("user", "usr", "u"),
        listOf(
            arg(
                "username",
                help = "Name of the user to mutate."
            ),
            arg(
                "action",
                false,
                "The action to perform, one of [show|delete|setpass|promote]. Defaults to show."
            ),
            arg(
                "additional",
                false,
                "Additional parameter to submit an argument to actions"
            )
        ),
        mapOf(
            null to "Performs various actions with users"
        )
    )

    override fun execute(name: String, args: Map<String, String?>) {
        val username = args.getValue("username")!!
        val action = args["action"] ?: "show"
        val additional = args["additional"]
        when (action) {
            "show" -> showUser(username)
            "delete" -> deleteUser(username)
            "setpass" -> setPassword(username, additional)
            "promote" -> promoteUser(username, additional)
        }
    }

    private fun showUser(username: String) {
        DogbinCli.Companion.Globals.getStore().transactional {
            val usr = XdUser.find(username)!!
            println("Username: ${usr.username}")
            println("Role: ${usr.role.name}")
            println("XdId: ${usr.xdId}")
            println("Created at: ${usr.created.format()}")
        }
    }

    private fun deleteUser(username: String) {
        DogbinCli.Companion.Globals.getStore().transactional {
            val usr = XdUser.find(username)!!
            print("Do you really want to delete ${usr.username}? This cannot be undone. [Y/n] ")
            val answer = readLine()
            if (answer == "Y") {
                usr.delete()
            }
        }
    }

    private fun setPassword(username: String, password: String?) {
        DogbinCli.Companion.Globals.getStore().transactional {
            val usr = XdUser.find(username)!!
            val pwd = if (password == null) {
                print("Enter a new password: ")
                readLine() ?: error("No password entered")
            } else password
            usr.password = pwd
        }
    }

    private fun promoteUser(username: String, newRole: String?) {
        DogbinCli.Companion.Globals.getStore().transactional {
            XdUser.find(username)!!.apply {
                role = when(newRole) {
                    "admin" -> XdUserRole.ADMIN
                    "mod" -> XdUserRole.MOD
                    else -> error("Unknown role $newRole")
                }
            }
        }
    }
}