package net.bjoernpetersen.musicbot.internal.image

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import java.io.IOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.spi.image.AlbumArtSupplier
import net.bjoernpetersen.musicbot.spi.image.ImageCache
import net.bjoernpetersen.musicbot.spi.image.ImageData
import net.bjoernpetersen.musicbot.spi.image.ImageLoader
import net.bjoernpetersen.musicbot.spi.plugin.PluginLookup
import net.bjoernpetersen.musicbot.spi.plugin.Provider

private const val CACHE_EXPIRATION_MINUTES = 10L
private const val CACHE_SIZE = 256L

internal class DefaultImageCache @Inject private constructor(
    private val pluginLookup: PluginLookup,
    imageCacheLoader: ImageCacheLoader
) : ImageCache {
    private val logger = KotlinLogging.logger { }

    private val cache: LoadingCache<String, ImageData> = CacheBuilder.newBuilder()
        .expireAfterAccess(CACHE_EXPIRATION_MINUTES, TimeUnit.MINUTES)
        .maximumSize(CACHE_SIZE)
        .build(imageCacheLoader)

    override fun getLocal(providerId: String, songId: String): ImageData? {
        val provider = pluginLookup.lookup<Provider>(providerId)
        if (provider == null) {
            logger.warn { "Could not find provider with ID $providerId" }
            return null
        }

        return if (provider is AlbumArtSupplier) {
            provider.getAlbumArt(songId)
        } else null
    }

    override fun getRemote(url: String): ImageData? {
        return try {
            cache[url]
        } catch (e: ExecutionException) {
            null
        }
    }
}

private class ImageCacheLoader @Inject private constructor(
    private val imageLoader: ImageLoader
) : CacheLoader<String, ImageData>() {
    override fun load(key: String): ImageData {
        return imageLoader[key] ?: throw IOException("Album art not found")
    }
}
