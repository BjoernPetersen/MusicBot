package com.github.bjoernpetersen.jmusicbot.config;

import com.github.bjoernpetersen.jmusicbot.MusicBot;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Collection of default config entries used by the core library.
 */
public final class DefaultConfigEntry {

  /**
   * The folder in which to look for plugin files.
   */
  public final Config.StringEntry pluginFolder;
  /**
   * The name of the default suggester which is used if the queue is empty.
   */
  public final Config.StringEntry suggester;

  private DefaultConfigEntry(Config config) {
    pluginFolder = config.stringEntry(
        MusicBot.class,
        "pluginFolder",
        "This is where the application looks for plugin files",
        "plugins"
    );
    suggester = config.stringEntry(
        MusicBot.class,
        "suggester",
        "Suggests songs if queue is empty.",
        null
    );
  }

  public List<? extends Config.Entry> getEntries() {
    return Collections.unmodifiableList(
        Arrays.asList(pluginFolder, suggester)
    );
  }

  public static DefaultConfigEntry get(Config config) {
    return new DefaultConfigEntry(config);
  }


}
