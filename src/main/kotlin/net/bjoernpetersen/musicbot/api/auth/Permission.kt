package net.bjoernpetersen.musicbot.api.auth

import com.google.common.annotations.Beta
import net.bjoernpetersen.musicbot.api.config.ConfigSerializer
import net.bjoernpetersen.musicbot.api.config.DeserializationException
import net.bjoernpetersen.musicbot.spi.plugin.Suggester

/**
 * User permissions to perform certain actions.
 * @param label A unique, human-readable label for this permission.
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
    @Default
    PAUSE("pause"),

    /**
     * Put new songs into the queue.
     */
    @Default
    ENQUEUE("enqueue"),

    /**
     * Songs enqueued by users without this permission do not affect suggestions.
     */
    @Default
    ALTER_SUGGESTIONS("alter_suggestions"),

    /**
     * Change the bot volume.
     */
    CHANGE_VOLUME("change_volume"),

    /**
     * Shut down the bot.
     */
    EXIT("exit");

    companion object : ConfigSerializer<Permission> {
        private val defaultPermissions: Set<Permission> by lazy {
            values().filterTo(HashSet()) { permission ->
                Permission::class.java
                    .getField(permission.name)
                    .isAnnotationPresent(Default::class.java)
            }
        }

        /**
         * Finds the permission with the specified label.
         * @throws IllegalArgumentException if there is no Permission with that label
         */
        @JvmStatic
        fun matchByLabel(label: String): Permission = Permission.values()
            .firstOrNull { it.label == label }
            ?: throw IllegalArgumentException("Unknown permission label: $label")

        /**
         * Gets the standard set of default permissions.
         */
        @Beta
        @JvmStatic
        fun getDefaults(): Set<Permission> = defaultPermissions

        override fun serialize(obj: Permission): String = obj.label
        override fun deserialize(string: String): Permission {
            return try {
                matchByLabel(string)
            } catch (e: IllegalArgumentException) {
                throw DeserializationException(e)
            }
        }
    }
}

@Beta
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
private annotation class Default
