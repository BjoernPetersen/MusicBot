package com.github.bjoernpetersen.jmusicbot;

import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class InitStateWriter {

  @Nonnull
  private static final Logger log = Logger.getLogger(InitStateWriter.class.getName());

  @Nonnull
  public static final InitStateWriter NO_OP = new InitStateWriter() {
    @Override
    public void begin(String pluginName) {
    }

    @Override
    public void state(String state) {
    }

    @Override
    public void warning(String warning) {
    }
  };

  @Nonnull
  public static final InitStateWriter LOG = new InitStateWriter() {
    @Nullable
    private String activePlugin;

    @Override
    public void begin(String pluginName) {
      this.activePlugin = pluginName;
    }

    @Override
    public void state(String state) {
      log.info(activePlugin + ": " + state);
    }

    @Override
    public void warning(String warning) {
      log.warning(activePlugin + ": " + warning);
    }
  };

  public abstract void begin(String pluginName);

  public abstract void state(String state);

  public abstract void warning(String warning);

  public void close() {
  }
}
