package net.bjoernpetersen.musicbot.internal.auth

internal interface RefreshClaimDatabase {
    fun getClaim(userId: String): String
    fun invalidateClaim(userId: String)
}
