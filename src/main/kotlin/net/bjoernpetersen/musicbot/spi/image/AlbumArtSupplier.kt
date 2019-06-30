package net.bjoernpetersen.musicbot.spi.image

/**
 * Capable of supplying album arts.
 *
 * Providers may implement this interface to serve local album art images.
 */
interface AlbumArtSupplier {
    /**
     * Gets an album art for the specified song ID.
     */
    fun getAlbumArt(songId: String): ImageData?
}
