package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface PlaybackStateListener {

  enum PlaybackState {
    PLAY, PAUSE // TODO add broken?
  }

  /**
   * Notify the listener that the PlaybackState has changed.
   *
   * @param state a PlaybackState
   */
  void notify(@Nonnull PlaybackState state);
}
