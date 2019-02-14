package net.bjoernpetersen.musicbot.api.auth

import com.google.common.annotations.Beta

@Beta
object DefaultPermissions {

    var defaultPermissions: Set<Permission> = Permission.getDefaults()
}
