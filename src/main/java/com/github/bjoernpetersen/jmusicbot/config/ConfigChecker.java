package com.github.bjoernpetersen.jmusicbot.config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@FunctionalInterface
public interface ConfigChecker {

  /**
   * Checks whether the given value is valid.
   *
   * @param value a config value
   * @return an warning message, or null
   */
  @Nullable
  String check(@Nonnull String value);
}
