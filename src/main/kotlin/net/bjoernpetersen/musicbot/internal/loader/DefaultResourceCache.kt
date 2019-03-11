package net.bjoernpetersen.musicbot.internal.loader

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.loader.ResourceCache
import net.bjoernpetersen.musicbot.spi.loader.SongLoader
import net.bjoernpetersen.musicbot.spi.plugin.PluginLookup
import java.time.Duration
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class DefaultResourceCache @Inject private constructor(
    loader: CacheSongLoader
) : ResourceCache, CoroutineScope {

    private val logger = KotlinLogging.logger {}

    private var isClosed = false
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val cleanupJob = SupervisorJob(job)
    private val cleanupScope =
        CoroutineScope(coroutineContext + cleanupJob + CoroutineExceptionHandler { _, throwable ->
            logger.error(throwable) { "Exception during cleanup" }
        })

    @Suppress("EXPERIMENTAL_API_USAGE")
    private val cache = CacheBuilder.newBuilder()
        .maximumSize(128)
        .expireAfterAccess(1, TimeUnit.HOURS)
        .removalListener<Song, Deferred<Resource>> {
            cleanupScope.launch {
                val deferred = it.value
                if ((deferred.isCompleted || deferred.isActive) && !deferred.isCancelled) {
                    deferred.join()
                    if (deferred.getCompletionExceptionOrNull() == null) {
                        val resource = deferred.getCompleted()
                        resource.free()
                    }
                }
            }
        }
        .build(loader)

    override suspend fun get(song: Song): Resource {
        if (isClosed) throw IllegalStateException("ResourceCache is closed")
        return coroutineScope {
            withContext(coroutineContext) {
                val asyncResource = cache[song]
                val resource = asyncResource.await()
                if (resource.isValid)
                    resource
                else {
                    cache.invalidate(song)
                    cache[song].await()
                }
            }
        }
    }

    override suspend fun close() {
        isClosed = true

        cache.invalidateAll()
        cache.cleanUp()

        coroutineScope {
            withContext(coroutineContext) {
                logger.info { "Waiting for resource cache to clean up. This may take up to a minute." }
                withTimeout(Duration.ofMinutes(1).toMillis()) {
                    try {
                        cleanupJob.children.forEach { it.join() }
                    } catch (e: TimeoutCancellationException) {
                        logger.warn { "Resource cache clean up timed out after one minute." }
                    }
                }
            }
        }

        job.cancel()

        logger.info { "Resource cache closed." }
    }
}

private class CacheSongLoader @Inject constructor(
    private val songLoader: SongLoader,
    private val pluginLookup: PluginLookup
) : CacheLoader<Song, Deferred<Resource>>(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    @Suppress("DeferredIsResult")
    override fun load(key: Song): Deferred<Resource> {
        return async(start = CoroutineStart.LAZY) {
            val provider = pluginLookup.lookup(key.provider)!!
            songLoader.load(provider, key)
        }
    }
}
