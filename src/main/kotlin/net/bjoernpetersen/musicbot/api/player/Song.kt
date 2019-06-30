package net.bjoernpetersen.musicbot.api.player

import net.bjoernpetersen.musicbot.api.image.ImageServerConstraints
import net.bjoernpetersen.musicbot.api.plugin.NamedPlugin
import net.bjoernpetersen.musicbot.spi.image.AlbumArtSupplier
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.id
import java.util.Base64

data class Song @Deprecated("Use Dsl instead") internal constructor(
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

@ExperimentalSongDsl
class MutableSong internal constructor(val id: String, val provider: Provider) {
    private val namedPlugin = provider.toNamedPlugin()
    lateinit var title: String
    lateinit var description: String
    var duration: Int? = null
    private var albumArtPath: String? = null

    fun serveLocalImage() {
        albumArtPath =
            "${ImageServerConstraints.LOCAL_PATH}/${namedPlugin.id.encode()}/${id.encode()}"
    }

    fun serveRemoteImage(url: String) {
        albumArtPath = remoteToLocalPath(url)
    }

    internal fun toSong(): Song {
        if (!this::title.isInitialized)
            throw IllegalStateException("Title not set")
        if (!this::description.isInitialized)
            throw IllegalStateException("Description not set")
        return Song(id, namedPlugin, title, description, duration, albumArtPath)
    }
}

fun Provider.toNamedPlugin(): NamedPlugin<Provider> = NamedPlugin(id.qualifiedName!!, subject)

@ExperimentalSongDsl
fun AlbumArtSupplier.song(id: String, configure: MutableSong.() -> Unit): Song {
    val mutable = MutableSong(id, this)
    mutable.serveLocalImage()
    mutable.configure()
    return mutable.toSong()
}

@ExperimentalSongDsl
fun Provider.song(id: String, configure: MutableSong.() -> Unit): Song {
    val mutable = MutableSong(id, this)
    mutable.configure()
    return mutable.toSong()
}

@Experimental
annotation class ExperimentalSongDsl
