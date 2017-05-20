package com.github.bjoernpetersen.jmusicbot.config;

import com.github.bjoernpetersen.jmusicbot.MusicBot;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultConfigEntry {

  public final Config.StringEntry pluginFolder;
  public final Config.StringEntry suggester;

  private DefaultConfigEntry(Config config) {
    pluginFolder = config.stringEntry(MusicBot.class, "pluginFolder", "TODO", "plugins");
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
