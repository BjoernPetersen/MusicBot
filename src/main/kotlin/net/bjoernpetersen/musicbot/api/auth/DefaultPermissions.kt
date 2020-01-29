package net.bjoernpetersen.musicbot.api.auth

import com.google.common.annotations.Beta

/**
 * Accessor object for default permissions.
 */
@Beta
object DefaultPermissions {

    /**
     * A set of all default permissions.
     */
    @Suppress("MemberNameEqualsClassName")
    @Deprecated("Use value", ReplaceWith("value"))
    var defaultPermissions: Set<Permission>
        get() = value
        set(value) {
            this.value = value
        }

    var value: Set<Permission> = Permission.getDefaults()
}
