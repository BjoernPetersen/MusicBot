package net.bjoernpetersen.musicbot

/**
 * Constraints for the server implementation.
 */
object ServerConstraints {

    /**
     * The port to bind the REST API on.
     */
    const val port = 42945

    /**
     * Constraints for broadcasting the bot address.
     */
    object Broadcast {

        // TODO: the message should be more meaningful and in JSON format
        const val message = "MusicBot"
        const val groupAdress = "224.0.0.142"
        const val port = ServerConstraints.port
    }
}
