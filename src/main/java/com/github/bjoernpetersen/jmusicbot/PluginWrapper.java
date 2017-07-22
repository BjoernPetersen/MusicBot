package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.config.Config;
import java.util.List;
import java.util.function.BiConsumer;
import javax.annotation.Nonnull;

/**
 * Wrapper for a plugin which provides access to the plugin's state and delegates all calls to the
 * wrapped instance.
 *
 * @param <T> the type of the wrapped plugin
 */
public interface PluginWrapper<T extends Plugin> extends Plugin {

  @Nonnull
  State getState();

  /**
   * Convenience method to check whether the current state is {@link State#ACTIVE}.
   *
   * @return whether the wrapped plugin is active
   */
  default boolean isActive() {
    return getState() == State.ACTIVE;
  }

  @Nonnull
  T getWrapped();

  @Nonnull
  List<? extends Config.Entry> getConfigEntries();

  void addStateListener(@Nonnull BiConsumer<State, State> listener);

  void removeStateListener(@Nonnull BiConsumer<State, State> listener);
}
