package net.bjoernpetersen.musicbot.spi.image

object ImageServerConstraints {
    /**
     * The port to bind the API on.
     */
    const val PORT = 42946

    /**
     * The path to serve (cached) remote images on.
     *
     * The actual full path will be `$REMOTE_PATH/BASE_64_URL`.
     */
    const val REMOTE_PATH = "/remote"

    /**
     * The path to serve local images on (supplied by plugins).
     *
     * The actual full path will be `$REMOTE_PATH/BASE_64_PROVIDER_ID/BASE_64_SONG_ID`.
     */
    const val LOCAL_PATH = "/local"
}
