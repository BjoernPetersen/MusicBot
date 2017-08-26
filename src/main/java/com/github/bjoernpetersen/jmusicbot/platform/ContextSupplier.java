package com.github.bjoernpetersen.jmusicbot.platform;

import android.content.Context;
import javax.annotation.Nonnull;

/**
 * Supplies an android application context.
 */
@FunctionalInterface
public interface ContextSupplier {

  @Nonnull
  Context supply();
}
