package com.github.bjoernpetersen.jmusicbot.platform;

import android.content.Context;
import javax.annotation.Nonnull;

/**
 * Supplies an android application context.
 */
@FunctionalInterface
public interface ContextSupplier {

  /**
   * Gets the application context of the currently running Android app.
   *
   * @return a {@link Context}
   */
  @Nonnull
  Context supply();
}
