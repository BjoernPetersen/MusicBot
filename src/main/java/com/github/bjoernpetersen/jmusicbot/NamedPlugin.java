package com.github.bjoernpetersen.jmusicbot;

import javax.annotation.Nonnull;

/**
 * Plugin that is identified by its name. Usually presented to the user in some way.
 */
public interface NamedPlugin extends Plugin {

  /**
   * A short, unique name for this plugin. No spaces. Only needs to be unique for the same kind of
   * plugin.
   *
   * @return a name
   */
  @Nonnull
  String getName();

  /**
   * An arbitrary, human readable name for this plugin.
   *
   * @return a name
   */
  @Nonnull
  String getReadableName();

}
