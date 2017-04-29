package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.InitializationException;
import com.github.bjoernpetersen.jmusicbot.Plugin;
import java.util.Collection;
import javax.annotation.Nonnull;

public interface PlaybackFactory extends Plugin {

  /**
   * <p>Initializes the PlaybackFactory. After this call, the factory should be operational until
   * {@link #close()} is called.</p>
   *
   * @throws InitializationException if anything goes wrong
   */
  void initialize() throws InitializationException;

  /**
   * Gets the PlaybackFactories this is implementing.
   *
   * @return a collection of PlaybackFactory interfaces
   */
  @Nonnull
  Collection<Class<? extends PlaybackFactory>> getBases();
}
