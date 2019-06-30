package net.bjoernpetersen.musicbot.spi.image

/**
 * Cache for images.
 *
 * The actual image loading is delegated to either a plugin or an [ImageLoader].
 */
interface ImageCache {
    /**
     * Gets a local image, supplied by the provider with the specified [providerId].
     * The provider needs to implement [AlbumArtSupplier] for this to work.
     */
    fun getLocal(providerId: String, songId: String): ImageData?

    /**
     * Gets a remote image, possibly from a cache.
     */
    fun getRemote(url: String): ImageData?
}
