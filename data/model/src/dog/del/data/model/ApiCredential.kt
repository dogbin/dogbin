package dog.del.data.model

import dog.del.commons.Date

interface ApiCredential<U : User<*>> {
    // We never store actual, full api keys
    var keyHash: String
    // User chosen name for the key to easily identify it in the dashboard
    var name: String

    // The user this key belongs to
    var user: U

    // Creation date/time of the key
    var created: Date

    // Actions which can be performed using this key
    var canCreateDocuments: Boolean
    var canUpdateDocuments: Boolean
    var canDeleteDocuments: Boolean
    var canListDocuments: Boolean
}