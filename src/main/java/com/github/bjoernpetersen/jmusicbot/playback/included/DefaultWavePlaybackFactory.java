package com.github.bjoernpetersen.jmusicbot.playback.included;

import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.playback.Playback;
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * PlaybackFactory capable of playing WAVE files on any standard JVM.
 */
public final class DefaultWavePlaybackFactory implements WavePlaybackFactory {

  @Nonnull
  @Override
  public List<Config.Entry> initializeConfigEntries(@Nonnull Config config) {
    return Collections.emptyList();
  }

  @Override
  public void initialize() {
  }

  @Override
  public void destructConfigEntries() {
  }

  @Override
  public void close() throws IOException {
  }

  @Nonnull
  @Override
  public Playback createPlayback(@Nonnull File inputFile)
      throws UnsupportedAudioFileException, IOException {
    return new DefaultWavePlayback(new FileInputStream(inputFile));
  }

  @Nonnull
  @Override
  public Collection<Class<? extends PlaybackFactory>> getBases() {
    return Collections.singleton(WavePlaybackFactory.class);
  }

}
