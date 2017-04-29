package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.config.Config.Entry;
import com.github.bjoernpetersen.jmusicbot.config.DefaultConfigEntry;
import java.io.Closeable;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Base interface for all plugins. Usually this interface won't be directly implemented, but
 * extended first. Extending interfaces should define a "<code>initialization(...) throws
 * InitializationException</code>" method, which acts as the counterpart for {@link #close()}.
 */
public interface Plugin extends Closeable {

  /**
   * <p>Initializes the config entries for this plugin.</p>
   *
   * <p>This method should only return config entries introduced by this plugin. If config entries
   * from {@link DefaultConfigEntry} are used, they should be excluded from the returned list.</p>
   *
   * <p>If the plugin wants prevent a config entry from being viewed/edited by the user, it can omit
   * it in this list.</p>
   *
   * @param config the config to use
   * @return a list of all config entries relevant only to this plugin.
   */
  @Nonnull
  List<? extends Entry> initializeConfigEntries(@Nonnull Config config);

  /**
   * <p>Destruct all entries initialized in {@link #initializeConfigEntries(Config)}.</p>
   *
   * <p>Example of destroying a config entry called 'apiKey':</p>
   * <pre><code>
   * // marks the entry as 'abandoned', it may or may not be garbage collected in the future
   * apiKey.tryDestruct();
   * // makes sure the entry can be garbage collected, if there are no other references to
   * // it in the current JVM
   * apiKey = null;
   * </code></pre>
   */
  void destructConfigEntries();
}
