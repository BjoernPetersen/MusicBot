package net.bjoernpetersen.musicbot.internal.auth

import com.google.common.cache.CacheBuilder
import com.google.common.cache.LoadingCache
import net.bjoernpetersen.musicbot.api.cache.cacheLoader

private const val MAX_SIZE = 2048L
private const val INIT_CAPACITY = 64

internal class CachedRefreshClaimDatabase(
    private val delegate: RefreshClaimDatabase
) : RefreshClaimDatabase {
    private val cache: LoadingCache<String, String> = CacheBuilder.newBuilder()
        .maximumSize(MAX_SIZE)
        .initialCapacity(INIT_CAPACITY)
        .build(cacheLoader(delegate::getClaim))

    override fun getClaim(userId: String): String {
        return cache[userId]
    }

    override fun invalidateClaim(userId: String) {
        cache.invalidate(userId)
        delegate.invalidateClaim(userId)
    }
}
