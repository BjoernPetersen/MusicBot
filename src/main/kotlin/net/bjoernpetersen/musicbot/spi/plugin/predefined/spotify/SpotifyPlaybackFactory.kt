package net.bjoernpetersen.musicbot.spi.plugin.predefined.spotify

import net.bjoernpetersen.musicbot.api.plugin.Base
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory

/**
 * PlaybackFactory for playing Spotify songs.
 */
@Base
interface SpotifyPlaybackFactory : PlaybackFactory {
    /**
     * Loads the song with the specified ID.
     *
     * @param songId the song ID
     * @return a resource representing the loaded song
     */
    suspend fun loadSong(songId: String): Resource

    /**
     * Creates a Playback object for the specified song using the given resource.
     *
     * @param songId the song ID
     * @param resource the resource that was returned by [loadSong] for the same song ID
     * @return a Playback object for the song
     */
    suspend fun getPlayback(songId: String, resource: Resource): Playback
}
