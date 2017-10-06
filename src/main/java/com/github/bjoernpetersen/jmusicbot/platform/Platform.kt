package com.github.bjoernpetersen.jmusicbot.platform

import com.google.common.annotations.Beta
import java.net.URL
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
          when {
            it.contains("win") -> WINDOWS
            it.contains("linux") -> determineLinux()
            else -> UNKNOWN
          }
        }

    @JvmStatic
    private fun determineLinux(): Platform =
        System.getProperty("java.runtime.name").toLowerCase(Locale.US).let {
          if (it.contains("android")) ANDROID
          else LINUX
        }
  }
}

enum class Support {
  YES, NO, MAYBE
}

@Beta
interface HostServices {

  fun openBrowser(url: URL)
  @Throws(IllegalStateException::class)
  fun contextSupplier(): ContextSupplier
}
