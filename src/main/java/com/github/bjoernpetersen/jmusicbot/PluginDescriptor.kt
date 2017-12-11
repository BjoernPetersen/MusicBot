@file:JvmName("PluginDescriptor")

package com.github.bjoernpetersen.jmusicbot

import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory
import com.github.bjoernpetersen.jmusicbot.provider.Provider
import com.github.bjoernpetersen.jmusicbot.provider.Suggester

/**
 * Describes the type of this plugin. If the plugin implements multiple types, they will be comma-separated.
 */
fun Plugin.describePluginType(): String {
  val type = javaClass
  val types = arrayOf(PlaybackFactory::class.java, Provider::class.java, Suggester::class.java, AdminPlugin::class.java)
  return types
      .filter { it.isAssignableFrom(type) }
      .joinToString { it.simpleName }
}

/**
 * Returns the readable plugin name and a list of implemented plugin types.
 *
 * Example: "ExamplePlugin (Provider, Suggester)"
 */
fun Plugin.qualifiedReadableName(): String = "$readableName (${describePluginType()})"
