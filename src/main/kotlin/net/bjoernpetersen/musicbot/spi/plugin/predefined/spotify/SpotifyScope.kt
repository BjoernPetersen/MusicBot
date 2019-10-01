package net.bjoernpetersen.musicbot.spi.plugin.predefined.spotify

import net.bjoernpetersen.musicbot.api.config.ConfigSerializer
import net.bjoernpetersen.musicbot.api.config.SerializationException

/**
 * A Spotify API scope.
 *
 * @param id the scope ID as recognized by the API
 */
enum class SpotifyScope(val id: String) {
    // Playlists
    /**
     * Include collaborative playlists when requesting a user's playlists.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#playlist-read-collaborative)
     */
    PLAYLIST_READ_COLLABORATIVE("playlist-read-collaborative"),
    /**
     * Write access to a user's private playlists.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#playlist-modify-private)
     */
    PLAYLIST_MODIFY_PRIVATE("playlist-modify-private"),
    /**
     * Write access to a user's public playlists.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#playlist-modify-public)
     */
    PLAYLIST_MODIFY_PUBLIC("playlist-modify-public"),
    /**
     * Read access to user's private playlists.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#playlist-read-private)
     */
    PLAYLIST_READ_PRIVATE("playlist-read-private"),

    // Spotify Connect
    /**
     * Write access to a user’s playback state
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-modify-playback-state)
     */
    USER_MODIFY_PLAYBACK_STATE("user-modify-playback-state"),
    /**
     * Read access to a user’s currently playing track
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-read-currently-playing)
     */
    USER_READ_CURRENTLY_PLAYING("user-read-currently-playing"),
    /**
     * Read access to a user’s player state.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-read-playback-state)
     */
    USER_READ_PLAYBACK_STATE("user-read-playback-state"),

    // Users
    /**
     * Read access to user’s subscription details (type of user account).
     *
     * Also required for search.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-read-private)
     */
    USER_READ_PRIVATE("user-read-private"),
    /**
     * Read access to user’s email address.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-read-email)
     */
    USER_READ_EMAIL("user-read-email"),

    // Library
    /**
     * Write/delete access to a user's "Your Music" library.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-library-modify)
     */
    USER_LIBRARY_MODIFY("user-library-modify"),
    /**
     * Read access to a user's "Your Music" library.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-library-read)
     */
    USER_LIBRARY_READ("user-library-read"),

    // Follow
    /**
     * Write/delete access to the list of artists and other users that the user follows.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-follow-modify)
     */
    USER_FOLLOW_MODIFY("user-follow-modify"),
    /**
     * Read access to the list of artists and other users that the user follows.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-follow-read)
     */
    USER_FOLLOW_READ("user-follow-read"),

    // Listening History
    /**
     * Read access to a user’s recently played tracks.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-read-recently-played)
     */
    USER_READ_RECENTLY_PLAYED("user-read-recently-played"),
    /**
     * Read access to a user's top artists and tracks.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#user-top-read)
     */
    USER_TOP_READ("user-top-read"),

    // Playback
    /**
     * Control playback of a Spotify track. This scope is currently available to the Web Playback SDK.
     * The user must have a Spotify Premium account.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#streaming)
     */
    STREAMING("streaming"),
    /**
     * Remote control playback of Spotify.
     * This scope is currently available to Spotify iOS and Android SDKs.
     *
     * [Official docs](https://developer.spotify.com/documentation/general/guides/scopes/#app-remote-control)
     */
    APP_REMOTE_CONTROL("app-remote-control");

    companion object : ConfigSerializer<SpotifyScope> {
        private val scopeById: Map<String, SpotifyScope> by lazy { values().associateBy { it.id } }
        override fun serialize(obj: SpotifyScope): String {
            return obj.id
        }

        override fun deserialize(string: String): SpotifyScope {
            return scopeById[string] ?: throw SerializationException()
        }
    }
}
