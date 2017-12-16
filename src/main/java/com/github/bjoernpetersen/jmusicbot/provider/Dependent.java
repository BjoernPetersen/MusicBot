package com.github.bjoernpetersen.jmusicbot.provider;

import com.github.bjoernpetersen.jmusicbot.Plugin;
import javax.annotation.Nonnull;

/**
 * A type dependent on another type of plugin.
 *
 * @param <P> the dependency plugin type
 */
public interface Dependent<P extends Plugin> {

  /**
   * Register the dependencies for this Plugin.
   */
  void registerDependencies(@Nonnull DependencyReport<P> dependencyReport);
}
