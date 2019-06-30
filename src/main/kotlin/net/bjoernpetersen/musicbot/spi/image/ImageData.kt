package net.bjoernpetersen.musicbot.spi.image

/**
 * Data describing an image.
 *
 * @param type the MIME-type of the image
 * @param data the image data
 */
class ImageData(val type: String, val data: ByteArray)
