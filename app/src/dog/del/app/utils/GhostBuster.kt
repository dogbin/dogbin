package dog.del.app.utils

import com.github.benmanes.caffeine.cache.Caffeine
import com.mitchellbosecke.pebble.extension.Function
import com.mitchellbosecke.pebble.template.EvaluationContext
import com.mitchellbosecke.pebble.template.PebbleTemplate
import dog.del.app.metrics.DogbinMetrics
import dog.del.commons.toHex
import io.prometheus.client.cache.caffeine.CacheMetricsCollector
import java.util.zip.Adler32

/**
 * A simple, adler32 based cache buster for statically served resources
 */
class GhostBuster : Function {
    override fun getArgumentNames() = listOf(KEY_RESOURCE_PATH)

    override fun execute(
        args: MutableMap<String, Any>,
        self: PebbleTemplate,
        context: EvaluationContext,
        lineNumber: Int
    ): Any {
        val resourcePath = args[KEY_RESOURCE_PATH] as String
        return bust(resourcePath)
    }

    companion object {
        const val KEY_RESOURCE_PATH = "resource_path"

        private val classloader = ClassLoader.getSystemClassLoader()
        private val ghostBusterCache = Caffeine.newBuilder()
            .maximumSize(100)
            .recordStats()
            .build<String, String> { resourcePath ->
                val adler = Adler32()
                val b = classloader.getResourceAsStream(resourcePath.removePrefix("/"))?.readBytes()
                    ?: return@build resourcePath
                adler.update(b)
                val hash = adler.value.toHex()
                return@build "$resourcePath?gbuster=$hash"
            }.also {
                get<DogbinMetrics>().cacheMetrics.addCache("ghostBusterCache", it)
            }

        fun bust(resourcePath: String): String = ghostBusterCache[resourcePath]!!
    }
}