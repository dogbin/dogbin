package dog.del.data.base.model.caches

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlin.reflect.KProperty1

@Deprecated("")
class XdHighlighterCache(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<XdHighlighterCache>() {
        override val compositeIndices: List<List<KProperty1<XdHighlighterCache, *>>>
            get() = super.compositeIndices
    }

    // This refers to the FULL slug, including "file extension"
    var slug by xdRequiredStringProp()
    var docId by xdRequiredStringProp()
    var version by xdRequiredIntProp()
    var content by xdRequiredBlobStringProp()
    var language by xdStringProp()
    var extension by xdStringProp()

    var highlighterVersion by xdIntProp()
}