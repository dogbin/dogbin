package dog.del.data.model

import dog.del.commons.Date
import java.io.InputStream

interface Document<T: DocumentType, U: User<*>> {

    var slug: String

    var type: T

    var stringContent: String?
    var blobContent: InputStream?
    var version: Int

    var owner: U

    val created: Date
}

interface DocumentType