package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.platform.Platform;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Serves as a loader for platform dependent plugins. Allows plugins to choose the implementation
 * class to load based on the current platform. This interface should not be directly implemented.
 */
public interface PlatformPlugin<T extends Plugin> {

  @Nonnull
  String getReadableName();

  /**
   * Gets an instance of the represented plugin supporting the specified platform.
   *
   * @return an inactive plugin, or null if the platform is not supported
   */
  @Nullable
  T load(@Nonnull Platform platform);
}
