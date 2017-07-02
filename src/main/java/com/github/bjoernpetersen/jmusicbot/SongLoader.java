package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.playback.Playback;
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;

/**
 * <p>Prepares songs to be played by a {@link PlaybackFactory}.</p>
 *
 * <p>Implementations are generally provided by a {@link Provider}. As an example, an implementation
 * could download a song to a MP3 file so a PlaybackFactory can subsequently play the file.</p>
 *
 * <p>It is possible that no loading is need or even possible before the song is actually played. In
 * this case, the {@link #DUMMY} implementation may be used.</p>
 */
@FunctionalInterface
public interface SongLoader extends Loggable {

  /**
   * A SongLoader implementation that does nothing
   */
  SongLoader DUMMY = song -> true;

  /**
   * Loads a song.<br>
   *
   * This method can potentially download a song before it is being played. The song might never be
   * actually played, so this should not prepare a {@link Playback} object in any way.<br>
   *
   * This method will not be called on a UI Thread.
   *
   * @param song the song to load
   * @return whether the song has been loaded successfully
   */
  boolean load(Song song);
}
