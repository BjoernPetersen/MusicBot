package com.github.bjoernpetersen.jmusicbot.config;

import com.github.bjoernpetersen.jmusicbot.MusicBot;
import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Collection of default config entries used by the core library.
 */
public final class DefaultConfigEntry {

  /**
   * The folder in which to look for plugin files. Default: "plugins".
   */
  @Nonnull
  public final Config.StringEntry pluginFolder;

  private DefaultConfigEntry(Config config) {
    pluginFolder = config.stringEntry(
        MusicBot.class,
        "pluginFolder",
        "This is where the application looks for plugin files",
        "plugins",
        fileName -> {
          File file = new File(fileName);
          if (file.isFile()) {
            return "This is a file, must be a directory";
          }
          return null;
        }
    );
  }

  /**
   * Gets a list of all default entries.
   *
   * @return a list of entries
   */
  @Nonnull
  public List<? extends Config.Entry> getEntries() {
    return Collections.unmodifiableList(
        Collections.singletonList(pluginFolder)
    );
  }

  /**
   * Gets the DefaultConfigEntry instance for the specified config.
   *
   * @param config a config
   * @return a DefaultConfigEntry instance
   */
  public static DefaultConfigEntry get(@Nonnull Config config) {
    return new DefaultConfigEntry(config);
  }
}
