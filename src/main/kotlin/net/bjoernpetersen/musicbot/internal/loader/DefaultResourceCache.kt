package net.bjoernpetersen.musicbot.internal.loader

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.loader.ResourceCache
import net.bjoernpetersen.musicbot.spi.loader.SongLoader
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

internal class DefaultResourceCache @Inject private constructor(
    loader: CacheSongLoader) : ResourceCache {

    private val logger = KotlinLogging.logger {}
    private val pool = Executors.newWorkStealingPool()

    private val cache = CacheBuilder.newBuilder()
        .maximumSize(128)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .removalListener<Song, Resource> { it.value.free() }
        .build(loader)

    override fun get(song: Song): Future<Resource> {
        return pool.submit(Callable {
            try {
                val res = cache[song]
                if (res.isValid()) res
                else {
                    cache.invalidate(song)
                    cache[song]
                }
            } catch (e: ExecutionException) {
                when (e.cause) {
                    null -> throw e
                    else -> throw e.cause!!
                }
            }
        })
    }

    override fun close() {
        pool.shutdown()
        logger.info { "Awaiting loader pool termination. This may take up to a minute..." }
        if (!pool.awaitTermination(1, TimeUnit.MINUTES)) {
            throw IOException("Could not terminate loader pool!")
        }

        cache.invalidateAll()
        cache.cleanUp()
    }
}

private class CacheSongLoader @Inject constructor(
    private val songLoader: SongLoader,
    @Named("PluginClassLoader")
    private val classLoader: ClassLoader,
    private val pluginFinder: PluginFinder) : CacheLoader<Song, Resource>() {

    override fun load(key: Song): Resource {
        val provider = key.provider.findPlugin(classLoader, pluginFinder)
        return songLoader.startLoading(provider, key).get()
    }
}
