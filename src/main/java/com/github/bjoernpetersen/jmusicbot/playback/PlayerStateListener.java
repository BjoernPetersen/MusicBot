package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface PlayerStateListener {

  /**
   * Is called after a new PlayerState is applied.
   *
   * @param state the new state
   */
  void onChanged(@Nonnull PlayerState state);
}
