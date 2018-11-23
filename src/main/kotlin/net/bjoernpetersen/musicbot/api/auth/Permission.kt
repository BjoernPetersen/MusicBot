package net.bjoernpetersen.musicbot.api.auth

import net.bjoernpetersen.musicbot.spi.plugin.Suggester

/**
 * User permissions to perform certain actions.
 * @param label A human-readable label for this permission.
 */
enum class Permission(val label: String) {

    /**
     * The permission to skip the current song or remove songs from the queue.
     * Note that a user is always allowed to remove a song from the queue which he added himself.
     */
    SKIP("skip"),
    /**
     * The permission to remove songs from the upcoming suggestions of a suggester.
     *
     * Note that doing this will also trigger [Suggester.dislike], thus affecting future suggestions.
     */
    DISLIKE("dislike"),
    /**
     * The permission to move songs around in the queue.
     */
    MOVE("move"),
    /**
     * Pause/resume current song.
     */
    PAUSE("pause"),
    /**
     * Put new songs into the queue.
     */
    ENQUEUE("enqueue");

    companion object {
        /**
         * Finds the permission with the specified label.
         * @throws IllegalArgumentException if there is no Permission with that label
         */
        @JvmStatic
        fun matchByLabel(label: String): Permission {
            return Permission.values()
                .asSequence()
                .filter { it.label == label }
                .firstOrNull() ?: throw IllegalArgumentException()
        }
    }
}
