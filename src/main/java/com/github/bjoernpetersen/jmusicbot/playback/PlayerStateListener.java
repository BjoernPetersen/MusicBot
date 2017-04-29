package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface PlayerStateListener {

  /**
   * Is called before a new PlayerState is applied.
   *
   * @param old the old state
   * @param next the new state
   */
  default void onChange(@Nonnull PlayerState old, @Nonnull PlayerState next) {
  }

  /**
   * Is called after a new PlayerState is applied.
   *
   * @param state the new state
   */
  void onChanged(@Nonnull PlayerState state);
}
