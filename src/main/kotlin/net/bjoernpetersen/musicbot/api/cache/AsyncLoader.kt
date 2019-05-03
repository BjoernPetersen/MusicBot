package net.bjoernpetersen.musicbot.api.cache

import com.google.common.annotations.Beta
import com.google.common.cache.CacheLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/**
 * Guava CacheLoader storing deferred results as values.
 *
 * @param scope a scope in which to launch coroutines
 * @param syncLoad a lookup function that will be asynchronously called whenever [load] is called.
 */
@Beta
@Suppress("DeferredIsResult")
internal class AsyncLoader<K, V>(
    private val scope: CoroutineScope,
    private val syncLoad: suspend (key: K) -> V
) : CacheLoader<K, Deferred<V>>() {

    override fun load(key: K): Deferred<V> {
        return scope.async {
            syncLoad(key)
        }
    }
}
