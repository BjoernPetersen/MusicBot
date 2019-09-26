package net.bjoernpetersen.musicbot.spi.plugin.predefined.youtube

import net.bjoernpetersen.musicbot.api.plugin.Base
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.PlaybackFactory

/**
 * PlaybackFactory for playing YouTube songs/videos.
 */
@Base
interface YouTubePlaybackFactory : PlaybackFactory {

    /**
     * Loads the video with the specified ID.
     *
     * @param videoId a video ID
     * @return a resource which resulted from loading
     */
    suspend fun load(videoId: String): Resource

    /**
     * Creates a playback object using the specified resource.
     *
     * @param videoId a video ID
     * @param resource the resource which was created by [load]
     * @return a playback object
     */
    suspend fun createPlayback(videoId: String, resource: Resource): Playback
}
