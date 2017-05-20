package com.github.bjoernpetersen.jmusicbot.playback;

/**
 * Playback for a single song.
 */
public interface Playback extends AutoCloseable {

  void play();

  void pause();

  void waitForFinish() throws InterruptedException;
}
