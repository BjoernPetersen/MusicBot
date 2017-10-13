package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.config.Config.Entry;
import java.util.List;
import javax.annotation.Nonnull;

@FunctionalInterface
public interface Configurator {

  /**
   * Give the user the possibility to configure a list of config entries.
   *
   * @param plugin a plugin name
   * @param entries a list of config entries
   * @return false, if the user canceled the configuration
   */
  boolean configure(@Nonnull String plugin, @Nonnull List<? extends Entry> entries);
}
