package dog.del.cli.features

import dog.del.cli.DogbinCli
import dog.del.commons.format
import dog.del.data.base.model.document.XdDocument

class DocumentFeature : CliFeature {
    override val metadata = FeatureMetadata(
        listOf("document", "doc", "d"),
        listOf(
            arg(
                "slug",
                help = "The slug (id) of the document"
            ),
            arg(
                "action",
                false,
                "One of [show|delete]. Defaults to show."
            )
        ),
        mapOf(
            null to "Perform operations on documents."
        )
    )

    override fun execute(name: String, args: Map<String, String?>) {
        val slug = args["slug"]!!
        val action = args["action"] ?: "show"
        when (action) {
            "show" -> showDocument(slug)
            "delete" -> deleteDocument(slug)
        }
    }

    fun showDocument(slug: String) {
        DogbinCli.Companion.Globals.getStore().transactional(readonly = true) {
            val doc = XdDocument.find(slug)!!
            println("Slug: ${doc.slug}")
            println("Type: ${doc.type.name}")
            println("Owner: ${doc.owner.username}")
            println("XdId: ${doc.xdId}")
            println("Created: ${doc.created.format()}")
        }
    }

    fun deleteDocument(slug: String) {
        DogbinCli.Companion.Globals.getStore().transactional {
            val doc = XdDocument.find(slug)!!
            print("Do you really want to delete ${doc.slug}? This cannot be undone. [Y/n] ")
            val answer = readLine()
            if (answer == "Y") {
                doc.delete()
            }
        }
    }
}