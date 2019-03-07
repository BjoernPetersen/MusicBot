package net.bjoernpetersen.musicbot.api.async

import com.google.common.annotations.Beta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Suppress("EXPERIMENTAL_API_USAGE")
private class DeferredFuture<T>(private val deferred: Deferred<T>) : Future<T> {

    override fun isDone(): Boolean = deferred.isCompleted

    override fun get(): T = runBlocking {
        deferred.join()
        val exception = deferred.getCompletionExceptionOrNull()
        when (exception) {
            is CancellationException -> throw exception
            null -> deferred.getCompleted()
            else -> throw ExecutionException(exception)
        }
    }

    override fun get(timeout: Long, unit: TimeUnit): T = runBlocking {
        withTimeout(unit.toMillis(timeout)) {
            try {
                deferred.join()
                val exception = deferred.getCompletionExceptionOrNull()
                when (exception) {
                    is CancellationException -> throw exception
                    null -> deferred.getCompleted()
                    else -> throw ExecutionException(exception)
                }
            } catch (e: TimeoutCancellationException) {
                throw TimeoutException()
            }
        }
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return if (deferred.isActive) {
            if (mayInterruptIfRunning) true.also { deferred.cancel() }
            else false
        } else if (!deferred.isCompleted) {
            true.also { deferred.cancel() }
        } else false
    }

    override fun isCancelled(): Boolean = deferred.isCancelled
}

@Beta
fun <T> Deferred<T>.asFuture(): Future<T> = DeferredFuture(this)
