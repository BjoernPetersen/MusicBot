package net.bjoernpetersen.musicbot.api.cache

import com.google.common.cache.CacheLoader

/**
 * Creates a [CacheLoader] based on the specified [load] function.
 */
fun <K, V> cacheLoader(load: (key: K) -> V): CacheLoader<K, V> = object : CacheLoader<K, V>() {
    override fun load(key: K): V = load(key)
}
