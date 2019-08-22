package net.bjoernpetersen.musicbot.api.image

/**
 * Constraints related to album art image serving.
 */
object ImageServerConstraints {
    /**
     * The path to serve (cached) remote images on.
     *
     * The actual full path will be `$REMOTE_PATH/BASE_64_URL`.
     */
    const val REMOTE_PATH = "/image/remote"

    /**
     * The path to serve local images on (supplied by plugins).
     *
     * The actual full path will be `$REMOTE_PATH/BASE_64_PROVIDER_ID/BASE_64_SONG_ID`.
     */
    const val LOCAL_PATH = "/image/local"
}
