package net.bjoernpetersen.musicbot.internal.loader

import com.google.inject.AbstractModule
import com.google.inject.Guice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeout
import kotlinx.coroutines.withContext
import net.bjoernpetersen.musicbot.api.config.Config
import net.bjoernpetersen.musicbot.api.module.DefaultSongLoaderModule
import net.bjoernpetersen.musicbot.api.player.Song
import net.bjoernpetersen.musicbot.api.player.song
import net.bjoernpetersen.musicbot.api.plugin.IdBase
import net.bjoernpetersen.musicbot.api.plugin.management.LogInitStateWriter
import net.bjoernpetersen.musicbot.spi.loader.Resource
import net.bjoernpetersen.musicbot.spi.loader.ResourceCache
import net.bjoernpetersen.musicbot.spi.plugin.Playback
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import net.bjoernpetersen.musicbot.spi.plugin.PluginLookup
import net.bjoernpetersen.musicbot.spi.plugin.Provider
import net.bjoernpetersen.musicbot.spi.plugin.management.ProgressUpdater
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

private fun createCache(): ResourceCache = Guice
    .createInjector(DefaultSongLoaderModule(), DummyPluginLookupModule)
    .getInstance(DefaultResourceCache::class.java)

class DefaultResourceCacheTest {
    @AfterEach
    fun resetProvider() {
        DummyProvider.loadDuration = Duration.ofMillis(50)
        DummyProvider.freeDuration = Duration.ofMillis(50)
    }

    @Test
    fun `all cleaned up`() {
        val cache: ResourceCache = createCache()

        val size = 1000
        val resources: List<Resource> = runBlocking {
            (1..size)
                .asSequence()
                .map { it.toString() }
                .map { runBlocking { DummyProvider.lookup(it) } }
                .map { async { cache.get(it) } }
                .toList()
                .mapTo(ArrayList(size)) {
                    try {
                        it.await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw e
                    }
                }
        }

        assertEquals(size, resources.size)

        runBlocking {
            cache.close()
            resources.forEach { assertFalse(it.isValid) }
        }
    }

    @Test
    fun `clean up aborts with timeout`() {
        val cache = createCache()
        val resource = runBlocking {
            DummyProvider.freeDuration = Duration.ofMinutes(2)
            val song = DummyProvider.lookup("longFreeResource")
            cache.get(song)
        }

        assertThrows<TimeoutCancellationException> {
            runBlocking {
                withTimeout(Duration.ofSeconds(90)) {
                    cache.close()
                }
            }
        }

        assertTrue(resource.isValid)
    }

    @Test
    fun `get works`() {
        val cache = createCache()
        runBlocking {
            val song = DummyProvider.lookup("test")
            val resource = cache.get(song)
            assertTrue(resource.isValid)
        }
    }

    @Test
    fun `get refreshes`() {
        val cache = createCache()
        runBlocking {
            val song = DummyProvider.lookup("test")
            val resource = cache.get(song)
            resource.free()
            assertFalse(resource.isValid)
            val refreshed = cache.get(song)
            assertNotSame(resource, refreshed)
            assertTrue(refreshed.isValid)
        }
    }

    @Test
    fun `get on closed cache`() {
        val cache = createCache()
        runBlocking {
            val song = DummyProvider.lookup("test")
            cache.close()
            assertThrows<IllegalStateException> {
                runBlocking {
                    cache.get(song)
                }
            }
        }
    }

    @Test
    fun `get while closing`() {
        val cache = createCache()
        runBlocking {
            DummyProvider.freeDuration = Duration.ofSeconds(1)
            val song = DummyProvider.lookup("test")
            withContext(Dispatchers.Default) {
                cache.get(song)
                val closing = CompletableDeferred<Unit>()
                launch {
                    closing.complete(Unit)
                    cache.close()
                }

                closing.await()
                delay(50)
                assertThrows<IllegalStateException> {
                    runBlocking {
                        cache.get(song)
                    }
                }
            }
        }
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun initProvider() {
            runBlocking {
                DummyProvider.initialize(LogInitStateWriter())
            }
        }

        @AfterAll
        @JvmStatic
        fun closeProvider() {
            runBlocking {
                DummyProvider.close()
            }
        }
    }
}

private class DummyResource(private val freeDuration: Duration) : Resource {
    private val mutex = Mutex()
    override var isValid: Boolean = true

    override suspend fun free() {
        mutex.withLock {
            if (!isValid) return
            delay(freeDuration)
            isValid = false
        }
    }
}

private object DummyPluginLookupModule : AbstractModule() {
    override fun configure() {
        bind(PluginLookup::class.java).toInstance(DummyProviderLookup)
    }
}

@Suppress("UNCHECKED_CAST")
private object DummyProviderLookup : PluginLookup {

    override fun <T : Plugin> lookup(id: String): T {
        return DummyProvider as T
    }

    override fun <T : Plugin> lookup(base: KClass<T>): T {
        return DummyProvider as T
    }
}

@IdBase("Dummy")
private object DummyProvider : Provider, CoroutineScope {

    override val name = "DummyProvider"
    override val description = ""
    override val subject
        get() = name

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    var loadDuration: Duration = Duration.ofMillis(50)
    var freeDuration: Duration = Duration.ofMillis(50)

    override suspend fun search(query: String, offset: Int): List<Song> {
        TODO("not implemented")
    }

    override suspend fun lookup(id: String) = song(id) {
        title = id
        description = ""
    }

    override suspend fun supplyPlayback(song: Song, resource: Resource): Playback {
        TODO("not implemented")
    }

    override suspend fun loadSong(song: Song): Resource {
        return coroutineScope {
            withContext(coroutineContext) {
                delay(loadDuration)
                DummyResource(freeDuration)
            }
        }
    }

    override fun createConfigEntries(config: Config): List<Config.Entry<*>> {
        TODO("not implemented")
    }

    override fun createSecretEntries(secrets: Config): List<Config.Entry<*>> {
        TODO("not implemented")
    }

    override fun createStateEntries(state: Config) {
        TODO("not implemented")
    }

    override suspend fun initialize(progressUpdater: ProgressUpdater) {
        job = Job()
    }

    override suspend fun close() {
        job.cancel()
    }
}
