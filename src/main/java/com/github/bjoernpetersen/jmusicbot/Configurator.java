package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.config.Config.Entry;
import java.util.List;
import javax.annotation.Nonnull;

@FunctionalInterface
public interface Configurator {

  enum Result {
    /**
     * OK, everything is fine, continue.
     */
    OK,
    /**
     * Abort MusicBot initialization.
     */
    CANCEL,
    /**
     * I do not want to configure this entries, disable the plugin.
     */
    DISABLE
  }

  /**
   * Give the user the possibility to configure a list of config entries.
   *
   * @param plugin a plugin name
   * @param entries a list of config entries
   * @return the result of the user decision
   */
  @Nonnull
  Result configure(@Nonnull String plugin, @Nonnull List<? extends Entry> entries);
}
