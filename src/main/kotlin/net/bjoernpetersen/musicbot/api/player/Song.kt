package net.bjoernpetersen.musicbot.api.player

import net.bjoernpetersen.musicbot.api.image.ImageServerConstraints
import net.bjoernpetersen.musicbot.api.plugin.NamedPlugin
import net.bjoernpetersen.musicbot.spi.image.AlbumArtSupplier
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.id
import java.util.Base64

/**
 * Information about a song, usually created by a [Provider].
 *
 * Equality is determined by the [id] in combination with the [provider].
 *
 * @param id an ID, unique for the associated [provider]
 * @param provider the provider this song originated from
 * @param title the song's title, which is the most important representation for humans
 * @param description further information about the song, usually the song's artist
 * @param duration the song duration in seconds
 * @param albumArtUrl the URL to the song's album art image
 * @param albumArtPath the URL path to the song's album relative to the bot's base URL
 */
data class Song @Deprecated("Use DSL instead") internal constructor(
    val id: String,
    val provider: NamedPlugin<Provider>,
    val title: String,
    val description: String,
    val duration: Int? = null,
    @Deprecated("Use albumArtPath instead")
    val albumArtUrl: String? = null,
    val albumArtPath: String? = null
) {

    @Deprecated("Use Dsl instead")
    @Suppress("DEPRECATION")
    @JvmOverloads
    constructor(
        id: String,
        provider: Provider,
        title: String,
        description: String,
        duration: Int? = null,
        albumArtUrl: String? = null
    ) : this(
        id = id,
        provider = provider.toNamedPlugin(),
        title = title,
        description = description,
        duration = duration,
        albumArtUrl = albumArtUrl,
        albumArtPath = albumArtUrl?.let(::remoteToLocalPath)
    )

    @Suppress("DEPRECATION")
    @JvmOverloads
    internal constructor(
        id: String,
        provider: NamedPlugin<Provider>,
        title: String,
        description: String,
        duration: Int? = null,
        albumArtPath: String? = null
    ) : this(
        id = id,
        provider = provider,
        title = title,
        description = description,
        duration = duration,
        albumArtUrl = null,
        albumArtPath = albumArtPath
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Song) return false

        if (id != other.id) return false
        if (provider != other.provider) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + provider.hashCode()
        return result
    }
}

private val encoder = Base64.getEncoder()

private fun String.encode(): String {
    return String(encoder.encode(toByteArray()), Charsets.UTF_8)
}

private fun remoteToLocalPath(remoteUrl: String): String {
    return "${ImageServerConstraints.REMOTE_PATH}/${remoteUrl.encode()}"
}

/**
 * Configuration object for the Song DSL.
 *
 * ### Required
 *
 * - [title]
 * - [description]
 *
 * ### Optional
 *
 * - [duration]
 * - [serveRemoteImage] (for local images just implement [AlbumArtSupplier])
 */
class SongConfiguration internal constructor(val id: String, val provider: Provider) {
    private val namedPlugin = provider.toNamedPlugin()
    /**
     * @see Song.title
     */
    lateinit var title: String
    /**
     * @see Song.description
     */
    lateinit var description: String
    /**
     * @see Song.duration
     */
    var duration: Int? = null
    private var albumArtPath: String? = null
    // TODO remove when albumArtUrl property is removed
    private var remoteUrl: String? = null

    /**
     * Configure the song to serve local album art images using the associated [provider] as an
     * [AlbumArtSupplier].
     */
    @Deprecated(
        "This is called automatically for implementations of AlbumArtSupplier",
        level = DeprecationLevel.WARNING
    )
    fun serveLocalImage() {
        albumArtPath =
            "${ImageServerConstraints.LOCAL_PATH}/${namedPlugin.id.encode()}/${id.encode()}"
    }

    /**
     * Serve the song's album art by loading the remote image found at [url].
     *
     * @param url a URL pointing to an album art image
     */
    @Suppress("unused")
    fun serveRemoteImage(url: String) {
        remoteUrl = url
        albumArtPath = remoteToLocalPath(url)
    }

    internal fun toSong(): Song {
        if (!this::title.isInitialized)
            throw IllegalStateException("Title not set")
        if (!this::description.isInitialized)
            throw IllegalStateException("Description not set")
        return Song(id, namedPlugin, title, description, duration, albumArtPath)
            .copy(albumArtUrl = remoteUrl)
    }
}

private fun Provider.toNamedPlugin(): NamedPlugin<Provider> =
    NamedPlugin(id.qualifiedName!!, subject)

/**
 * Create a song using the Song DSL and configure it to serve the album art using the calling
 * AlbumArtSupplier.
 *
 * @param id the song's [ID][Song.id]
 * @param configure a block in which to [configure the song][SongConfiguration]
 * @return the created song
 */
@Suppress("DEPRECATION")
fun AlbumArtSupplier.song(id: String, configure: SongConfiguration.() -> Unit): Song {
    val mutable = SongConfiguration(id, this)
    mutable.serveLocalImage()
    mutable.configure()
    return mutable.toSong()
}

/**
 * Create a song using the Song DSL.
 *
 * @param id the song's [ID][Song.id]
 * @param configure a block in which to [configure the song][SongConfiguration]
 * @return the created song
 */
fun Provider.song(id: String, configure: SongConfiguration.() -> Unit): Song {
    val mutable = SongConfiguration(id, this)
    mutable.configure()
    return mutable.toSong()
}
