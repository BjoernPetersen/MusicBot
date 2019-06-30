package net.bjoernpetersen.musicbot.spi.image

/**
 * Loads an image from a remote URL.
 */
interface ImageLoader {
    /**
     * Loads an image from the [url].
     *
     * @param url any valid URL
     * @return the image
     */
    operator fun get(url: String): ImageData?
}
