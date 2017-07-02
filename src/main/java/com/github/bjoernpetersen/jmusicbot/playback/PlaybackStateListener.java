package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;

@FunctionalInterface
public interface PlaybackStateListener {

  enum PlaybackState {
    PLAY, PAUSE
  }

  void notify(@Nonnull PlaybackState state);
}
