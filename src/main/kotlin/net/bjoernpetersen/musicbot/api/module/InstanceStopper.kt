package net.bjoernpetersen.musicbot.api.module

import com.google.inject.ConfigurationException
import com.google.inject.Injector
import com.google.inject.ProvisionException
import kotlin.reflect.KClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import net.bjoernpetersen.musicbot.api.plugin.management.PluginFinder
import net.bjoernpetersen.musicbot.spi.loader.ResourceCache
import net.bjoernpetersen.musicbot.spi.loader.SongLoader
import net.bjoernpetersen.musicbot.spi.player.Player

private const val CUSTOM_STOPPER_CAPACITY = 32

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
    private val additionalBefore: MutableSet<Stopper<*>> = HashSet(CUSTOM_STOPPER_CAPACITY)
    private val additionalAfter: MutableSet<Stopper<*>> = HashSet(CUSTOM_STOPPER_CAPACITY)

    private suspend fun <T> unstopped(action: suspend () -> T): T {
        if (stopped) throw IllegalStateException("stop() has already been called")
        return action()
    }

    private fun <T : Any> KClass<T>.lookup(): T = try {
        injector.getInstance(this.java)
    } catch (e: ConfigurationException) {
        logger.error(e) { "Could not find ${this.qualifiedName}" }
        throw IllegalStateException(e)
    } catch (e: ProvisionException) {
        logger.error(e) { "Could not provide instance of ${this.qualifiedName}" }
        throw IllegalStateException(e)
    }

    private suspend fun <T : Any> KClass<T>.withLookup(action: suspend (T) -> Unit) {
        val instance = try {
            this.lookup()
        } catch (e: IllegalStateException) {
            // The lookup method already logged the exception
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
     * @param close will be called to close the instance
     */
    fun <T : Any> register(
        type: Class<T>,
        before: Boolean = true,
        close: suspend (T) -> Unit
    ) = runBlocking<Unit> {
        unstopped {
            @Suppress("TooGenericExceptionCaught")
            val instance = try {
                injector.getInstance(type)
            } catch (e: RuntimeException) {
                throw IllegalArgumentException(e)
            }

            val set = if (before) additionalBefore else additionalAfter
            set.add(Stopper(instance, close))
        }
    }

    private suspend fun close(stopper: Stopper<*>) {
        close(stopper) { it() }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T : Any> close(instance: T, closer: suspend (T) -> Unit) {
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
    suspend fun stop(): Unit = unstopped {
        withContext(Dispatchers.Default) {
            additionalBefore.forEach { close(it) }

            Player::class.withLookup { close(it) { it.close() } }
            ResourceCache::class.withLookup { close(it) { it.close() } }
            SongLoader::class.withLookup { close(it) { it.close() } }
            PluginFinder::class.withLookup { finder ->
                finder.allPlugins().forEach { close(it) { it.close() } }
            }

            additionalAfter.forEach { close(it) }
        }

        stopped = true
    }
}

private data class Stopper<T : Any>(
    private val instance: T,
    private val close: suspend (T) -> Unit
) {

    suspend operator fun invoke() = close(instance)
}
