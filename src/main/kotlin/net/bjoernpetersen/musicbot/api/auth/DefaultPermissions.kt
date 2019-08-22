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
    var defaultPermissions: Set<Permission> = Permission.getDefaults()
}
