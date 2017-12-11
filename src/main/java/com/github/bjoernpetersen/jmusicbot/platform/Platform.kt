package com.github.bjoernpetersen.jmusicbot.platform

import com.github.bjoernpetersen.jmusicbot.Plugin
import com.google.common.annotations.Beta
import java.net.URL
import java.util.*

/**
 * Represents a Platform the MusicBot might run on.
 */
enum class Platform(
    /**
     * A human-readable name for this platform.
     */
    val readableName: String) {

  /**
   * Microsoft Windows.
   */
  WINDOWS("Windows"),
  /**
   * A Linux distribution (not further specified).
   */
  LINUX("Linux"),
  /**
   * The Android platform.
   */
  ANDROID("Android"),
  /**
   * Not an officially supported platform. Maybe it works, maybe it doesn't.
   */
  UNKNOWN("Unknown");

  override fun toString(): String = readableName

  companion object {
    @JvmStatic
    private val platform: Lazy<Platform> = lazy { determinePlatform() }

    /**
     * Gets the current platform.
     */
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

/**
 * Specifies whether a [Plugin] supports a [Platform].
 */
enum class Support {

  /**
   * The Platform is officially supported.
   */
  YES,
  /**
   * The Platform is definitely not supported.
   */
  NO,
  /**
   * The platform might be supported, but is not tested.
   */
  MAYBE
}

/**
 * Platform-dependent services.
 */
@Beta
interface HostServices {

  /**
   * Opens the specified URL in the user's default web browser.
   */
  fun openBrowser(url: URL)

  /**
   * If the current platform is Android, this returns a [ContextSupplier] which supplies the application context.
   * @throws if the current platform is not Android
   */
  @Throws(IllegalStateException::class)
  fun contextSupplier(): ContextSupplier
}
