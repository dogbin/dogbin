package dog.del.data.model

import java.io.InputStream

interface Document<T: DocumentType, U: User<*>> {

    var slug: String

    var type: T

    var stringContent: String?
    var blobContent: InputStream?
    var version: Int

    var owner: U
}

interface DocumentType