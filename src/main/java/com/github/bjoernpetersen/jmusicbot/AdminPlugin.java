package com.github.bjoernpetersen.jmusicbot;

import javax.annotation.Nonnull;

public interface AdminPlugin extends Plugin {

  void initialize(@Nonnull InitStateWriter writer, @Nonnull MusicBot musicBot)
      throws InitializationException;
}
