package com.github.bjoernpetersen.jmusicbot.platform

import java.util.*

enum class Platform(val readableName: String) {
  WINDOWS("Windows"), LINUX("Linux"), ANDROID("Android"), UNKNOWN("Unknown");

  companion object {
    @JvmStatic
    private val platform: Lazy<Platform> = lazy { determinePlatform() }

    @JvmStatic
    fun get(): Platform = platform.value

    @JvmStatic
    private fun determinePlatform(): Platform =
        // TODO use something like apache commons SystemUtils
        System.getProperty("os.name").toLowerCase(Locale.US).let {
          if (it.contains("win")) WINDOWS
          else if (it.contains("linux")) determineLinux()
          else UNKNOWN
        }

    @JvmStatic
    private fun determineLinux(): Platform =
        System.getProperty("java.runtime.name").toLowerCase(Locale.US).let {
          if (it.contains("android")) ANDROID
          else LINUX
        }
  }
}
