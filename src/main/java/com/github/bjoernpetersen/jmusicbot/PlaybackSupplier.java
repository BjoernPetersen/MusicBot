package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.playback.Playback;
import java.io.IOException;
import javax.annotation.Nonnull;

@FunctionalInterface
public interface PlaybackSupplier {

  /**
   * Supplies the playback object for the specified song.
   *
   * @param song a song
   * @return a Playback object
   * @throws IOException if the playback could not be created
   */
  @Nonnull
  Playback supply(Song song) throws IOException;
}
