package com.github.bjoernpetersen.jmusicbot;

import java.io.Closeable;
import javax.annotation.Nonnull;

@FunctionalInterface
public interface BroadcasterInitializer {

  @Nonnull
  Closeable initialize(int port, String groupAddress, String message)
      throws InitializationException;
}
