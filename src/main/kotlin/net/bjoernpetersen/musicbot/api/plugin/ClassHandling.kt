package net.bjoernpetersen.musicbot.api.plugin

import kotlin.reflect.KClass

/**
 * Remove variance modifier.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> KClass<out T>.fix(): KClass<T> = this as KClass<T>
