package com.github.bjoernpetersen.jmusicbot;

import javax.annotation.Nonnull;

public interface IdPlugin extends Plugin {

  /**
   * A short, unique ID for this plugin. No spaces. Only needs to be unique for the same kind of
   * plugin.
   *
   * @return an ID
   */
  @Nonnull
  String getId();
}
