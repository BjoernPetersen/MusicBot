package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.config.Config.Entry;
import com.github.bjoernpetersen.jmusicbot.platform.Platform;
import com.github.bjoernpetersen.jmusicbot.platform.Support;
import com.github.bjoernpetersen.jmusicbot.playback.PlaybackFactory;
import com.github.bjoernpetersen.jmusicbot.provider.DependencyMap;
import com.github.bjoernpetersen.jmusicbot.provider.DependencyReport;
import com.github.bjoernpetersen.jmusicbot.provider.NoSuchSongException;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public class TestProvider implements Provider {

  @Nonnull
  @Override
  public Support getSupport(@Nonnull Platform platform) {
    return Support.YES;
  }

  @Nonnull
  @Override
  public Class<? extends Provider> getBaseClass() {
    return TestProvider.class;
  }

  @Nonnull
  @Override
  public String getId() {
    return "testprovider";
  }

  @Nonnull
  @Override
  public String getReadableName() {
    return "TestProvider";
  }

  @Override
  public void registerDependencies(@Nonnull DependencyReport<PlaybackFactory> dependencyReport) {
  }

  @Override
  public void initialize(@Nonnull InitStateWriter initStateWriter,
      @Nonnull DependencyMap<PlaybackFactory> dependencies) {
  }

  @Nonnull
  @Override
  public List<Song> search(@Nonnull String query) {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public Song lookup(@Nonnull String id) throws NoSuchSongException {
    throw new NoSuchSongException();
  }

  @Nonnull
  @Override
  public List<? extends Entry> initializeConfigEntries(@Nonnull Config config) {
    return Collections.emptyList();
  }

  @Nonnull
  @Override
  public List<? extends Entry> getMissingConfigEntries() {
    return Collections.emptyList();
  }

  @Override
  public void destructConfigEntries() {
  }

  @Override
  public void close() throws IOException {
  }
}
