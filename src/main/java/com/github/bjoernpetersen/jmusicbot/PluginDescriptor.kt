@file:JvmName("PluginDescriptor")

package com.github.bjoernpetersen.jmusicbot

import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory
import com.github.bjoernpetersen.jmusicbot.provider.Provider
import com.github.bjoernpetersen.jmusicbot.provider.Suggester

fun Plugin.describePluginType(): String {
  val type = javaClass
  val types = arrayOf(PlaybackFactory::class.java, Provider::class.java, Suggester::class.java, AdminPlugin::class.java)
  return types
      .filter { it.isAssignableFrom(type) }
      .joinToString { it.simpleName }
}

fun Plugin.qualifiedReadableName(): String = "$readableName (${describePluginType()})"
