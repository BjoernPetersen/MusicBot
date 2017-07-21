package com.github.bjoernpetersen.jmusicbot;

import javax.annotation.Nonnull;

/**
 * Plugin that is identified by its name. Usually presented to the user in some way.
 *
 * @deprecated in favor of {@link IdPlugin}. Will be removed in 0.9.0.
 */
@Deprecated
public interface NamedPlugin extends IdPlugin {

  /**
   * A short, unique name for this plugin. No spaces. Only needs to be unique for the same kind of
   * plugin.
   *
   * @return a name
   * @deprecated in favor of {@link IdPlugin#getId()}
   */
  @Deprecated
  @Nonnull
  String getName();

  default String getId() {
    return getName();
  }
}
