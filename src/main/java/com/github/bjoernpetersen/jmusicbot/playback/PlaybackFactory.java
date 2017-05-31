package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.InitStateWriter;
import com.github.bjoernpetersen.jmusicbot.InitializationException;
import com.github.bjoernpetersen.jmusicbot.Plugin;
import java.util.Collection;
import javax.annotation.Nonnull;

public interface PlaybackFactory extends Plugin {

  /**
   * <p>Initializes the PlaybackFactory. After this call, the factory should be operational until
   * {@link #close()} is called.</p>
   *
   * @param initStateWriter a writer for initialization state messages
   * @throws InitializationException if anything goes wrong
   * @throws InterruptedException if the thread is interrupted while initializing
   */
  void initialize(@Nonnull InitStateWriter initStateWriter)
      throws InitializationException, InterruptedException;

  /**
   * Gets the PlaybackFactories this is implementing.
   *
   * @return a collection of PlaybackFactory interfaces
   */
  @Nonnull
  Collection<Class<? extends PlaybackFactory>> getBases();
}
