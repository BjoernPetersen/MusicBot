package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;

/**
 * Playback for a single song. Playback should not start before {@link #play()} is called the first time.
 */
public interface Playback extends AutoCloseable {

  /**
   * Provides the playback with a PlaybackStateListener.
   * The listener can be used to tell the player about external pause/resume events.
   *
   * @param listener a PlaybackStateListener
   */
  default void setPlaybackStateListener(@Nonnull PlaybackStateListener listener) {
  }

  /**
   * Resumes the playback. This might be called if the playback is already active.
   */
  void play();

  /**
   * Pauses the playback. This might be called if the playback is already paused.
   */
  void pause();

  void waitForFinish() throws InterruptedException;
}
