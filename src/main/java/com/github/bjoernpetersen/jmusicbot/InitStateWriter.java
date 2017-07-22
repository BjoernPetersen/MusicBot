package com.github.bjoernpetersen.jmusicbot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class InitStateWriter implements Loggable {

  @Nonnull
  public static final InitStateWriter NO_OP = new InitStateWriter() {
    @Override
    public void begin(@Nonnull String pluginName) {
    }

    @Override
    public void state(@Nonnull String state) {
    }

    @Override
    public void warning(@Nonnull String warning) {
    }
  };

  @Nonnull
  public static final InitStateWriter LOG = new InitStateWriter() {
    @Nullable
    private String activePlugin;

    @Override
    public void begin(@Nonnull String pluginName) {
      this.activePlugin = pluginName;
    }

    @Override
    public void state(@Nonnull String state) {
      logInfo(activePlugin + ": " + state);
    }

    @Override
    public void warning(@Nonnull String warning) {
      logWarning(activePlugin + ": " + warning);
    }
  };

  public abstract void begin(@Nonnull String pluginName);

  public abstract void state(@Nonnull String state);

  public abstract void warning(@Nonnull String warning);

  public void close() {
  }
}
