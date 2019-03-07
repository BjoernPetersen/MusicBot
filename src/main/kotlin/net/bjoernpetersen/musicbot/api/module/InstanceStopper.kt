package net.bjoernpetersen.musicbot.api.module

import com.google.inject.Injector
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.spi.loader.ResourceCache
import net.bjoernpetersen.musicbot.spi.loader.SongLoader
import net.bjoernpetersen.musicbot.spi.player.Player
import net.bjoernpetersen.musicbot.spi.plugin.Plugin
import java.io.Closeable
import kotlin.reflect.KClass

/**
 * Can be used to gracefully shut down an instance of the bot.
 *
 * In addition to the default set of interfaces, any number of closeable interfaces can be
 * registered via [register]. For each of those interfaces, the instance will be loaded from
 * the given [injector] and closed.
 *
 * @param injector an injector to look up all relevant MusicBot instances
 */
class InstanceStopper(private val injector: Injector) {

    private val logger = KotlinLogging.logger { }

    private var stopped = false
    private val additionalBefore: MutableSet<Stopper<*>> = HashSet(32)
    private val additionalAfter: MutableSet<Stopper<*>> = HashSet(32)

    private fun <T> unstopped(action: () -> T): T {
        if (stopped) throw IllegalStateException("stop() has already been called")
        return action()
    }

    private fun <T : Any> KClass<T>.lookup(): T = try {
        injector.getInstance(this.java)
    } catch (e: RuntimeException) {
        logger.error(e) { "Could not find ${this.qualifiedName}" }
        throw IllegalStateException(e)
    }

    private fun <T : Any> KClass<T>.withLookup(action: (T) -> Unit) {
        val instance = try {
            injector.getInstance(this.java)
        } catch (e: RuntimeException) {
            logger.error(e) { "Could not find ${this.qualifiedName}" }
            return
        }
        action(instance)
    }

    /**
     * Registers an additional [type] to look up via the [injector] and close either before or after
     * the instances of all default MusicBot interfaces.
     *
     * @param type the type for which to look up the implementation and close when [stop] is called
     * @param before whether to close the instance before the rest; default: `true`
     */
    fun register(type: Class<out Closeable>, before: Boolean = true): Unit = unstopped {
        register(type, before, Closeable::close)
    }

    /**
     * Registers an additional [type] to look up via the [injector] and close either before or after
     * the instances of all default MusicBot interfaces.
     *
     * @param type the type for which to look up the implementation and close when [stop] is called
     * @param before whether to close the instance before the rest; default: `true`
     * @param close will be called to close the instance
     */
    fun <T : Any> register(type: Class<T>, before: Boolean = true, close: (T) -> Unit): Unit {
        val instance = try {
            injector.getInstance(type)
        } catch (e: RuntimeException) {
            throw IllegalArgumentException(e)
        }

        val set = if (before) additionalBefore else additionalAfter
        set.add(Stopper(instance, close))
    }


    private fun close(stopper: Stopper<*>) {
        close(stopper) { it() }
    }

    private fun <T : Any> close(instance: T, closer: (T) -> Unit) {
        try {
            closer(instance)
        } catch (e: InterruptedException) {
            logger.error(e) { "Interrupted while closing ${instance::class.qualifiedName}" }
            throw e
        } catch (e: Throwable) {
            logger.error(e) { "Could not close ${instance::class.qualifiedName}" }
        }
    }

    /**
     * Stops all registered instances.
     */
    fun stop(): Unit = unstopped {
        additionalBefore.forEach(::close)

        Player::class.withLookup { close(it) { runBlocking { it.close() } } }
        ResourceCache::class.withLookup { close(it) { runBlocking { it.close() } } }
        SongLoader::class.withLookup { close(it) { runBlocking { it.close() } } }
        PluginFinder::class.withLookup { finder ->
            finder.allPlugins().forEach { close(it, Plugin::close) }
        }

        additionalAfter.forEach(::close)

        stopped = true
    }
}

private data class Stopper<T : Any>(private val instance: T, private val close: (T) -> Unit) {
    operator fun invoke() = close(instance)
}
