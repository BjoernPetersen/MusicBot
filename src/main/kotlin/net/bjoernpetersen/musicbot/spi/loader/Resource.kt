package net.bjoernpetersen.musicbot.spi.loader

import java.io.IOException

/**
 * A resource that is allocated when a song is loaded and needs to be deallocated at some point.
 *
 * One resource object may be reused any number of times as long as it is [valid][isValid]
 */
interface Resource {

    /**
     * Free this resource.
     *
     * Future [isValid] calls should never return `true` again.
     * This means you should introduce an `isFreed` field to short-circuit [isValid].
     *
     */
    @Throws(IOException::class)
    fun free()

    /**
     * Checks whether this resource is still valid and can be used.
     *
     * For example: a file resource may check whether the file still exists
     */
    fun isValid(): Boolean
}
