package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.github.bjoernpetersen.jmusicbot.config.Config.Entry;
import com.github.bjoernpetersen.jmusicbot.platform.Platform;
import com.github.bjoernpetersen.jmusicbot.platform.Support;
import com.github.zafarkhaja.semver.Version;
import java.io.Closeable;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * <p>Base interface for all plugins. Usually this interface won't be directly implemented, but
 * extended first. Extending interfaces should define a "<code>initialize(InitStateWriter, ...)
 * throws InitializationException</code>" method, which acts as the counterpart to {@link
 * #close()}.</p>
 *
 * Lifecycle:<br> <ol> <li>{@link #initializeConfigEntries(Config)}</li> <li>initialize(...)</li>
 * <li>{@link #close()}</li> <li>{@link #destructConfigEntries()}</li> </ol> <b>Note:</b> a
 * successful call to one of the initialization methods guarantees that the respective destruction
 * method will be called in the future.
 */
public interface Plugin extends Closeable {

  /**
   * <p>Initializes the config entries for this plugin.</p>
   *
   * <p>This method should only return config entries introduced by this plugin. If config entries
   * from {@link Config#getDefaults()} are used, they should be excluded from the returned
   * list.</p>
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
   * <p>Destruct and dereference all entries initialized in {@link #initializeConfigEntries(Config)}.</p>
   */
  void destructConfigEntries();

  /**
   * Gets the config entries which are not configured properly. Might be called multiple times.<br>
   *
   * This will be called between {@link #initializeConfigEntries(Config)} and initialize(...).
   *
   * <p>Note: Config.StringEntry.isNullOrError() might be helpful here.</p>
   *
   * @return a list of config entries
   */
  @Nonnull
  List<? extends Entry> getMissingConfigEntries();

  /**
   * An arbitrary, human readable name for this plugin.
   *
   * @return a name
   */
  @Nonnull
  String getReadableName();

  /**
   * Indicates whether the specified Platform is supported.
   *
   * @param platform the current Plaform
   * @return the support for the specified platform
   */
  @Nonnull
  Support getSupport(@Nonnull Platform platform);

  /**
   * Gets the minimum supported version of MusicBot.
   *
   * @return a version
   */
  @Nonnull
  default Version getMinSupportedVersion() {
    return MusicBot.getVersion();
  }

  /**
   * Gets the maximum supported version of MusicBot.
   *
   * @return a version of MusicBot
   */
  @Nonnull
  default Version getMaxSupportedVersion() {
    Version version = MusicBot.getVersion();
    if (getMinSupportedVersion().greaterThan(version)) {
      return getMinSupportedVersion();
    } else {
      return version;
    }
  }

  enum State {
    INACTIVE, CONFIG, ACTIVE
  }
}
