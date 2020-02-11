package dog.del.data.base.model.caches

import jetbrains.exodus.entitystore.Entity
import kotlinx.dnq.*
import kotlinx.dnq.query.filter
import kotlinx.dnq.query.firstOrNull

@Deprecated("")
class XdScreenshotCache(entity: Entity) : XdEntity(entity) {
    companion object : XdNaturalEntityType<XdScreenshotCache>() {
        fun find(path: String) = filter {
            it.path.eq(path)
        }.firstOrNull()
    }

    var path by xdRequiredStringProp(unique = true)
    var version by xdIntProp()
    var screenshotUrl by xdStringProp()
}
