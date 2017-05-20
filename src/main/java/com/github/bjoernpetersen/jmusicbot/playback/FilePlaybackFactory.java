package com.github.bjoernpetersen.jmusicbot.playback;

import java.io.File;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * An extension of the {@link PlaybackFactory} interface which accepts input files.
 */
public interface FilePlaybackFactory extends PlaybackFactory {

  /**
   * <p>Creates a playback object from the given input file.</p>
   *
   * <p>This method can perform blocking IO actions.</p>
   *
   * @param inputFile the input file with audio data
   * @return a Playback object
   * @throws UnsupportedAudioFileException if the format of the input stream is unsupported
   * @throws IOException if any IO error occurs
   */
  @Nonnull
  Playback createPlayback(@Nonnull File inputFile)
      throws UnsupportedAudioFileException, IOException;
}
