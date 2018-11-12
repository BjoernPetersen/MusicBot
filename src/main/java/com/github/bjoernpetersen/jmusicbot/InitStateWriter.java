package com.github.bjoernpetersen.jmusicbot;

import javax.annotation.Nonnull;

public interface InitStateWriter {

  @Nonnull
  InitStateWriter NO_OP = new InitStateWriter() {
    @Override
    public void begin(@Nonnull Plugin plugin) {
    }

    @Override
    public void state(@Nonnull String state) {
    }

    @Override
    public void warning(@Nonnull String warning) {
    }
  };

  void begin(@Nonnull Plugin plugin);

  void state(@Nonnull String state);

  void warning(@Nonnull String warning);

  default void close() {
  }
}
