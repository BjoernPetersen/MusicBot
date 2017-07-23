package com.github.bjoernpetersen.jmusicbot.platform;

import android.content.Context;
import javax.annotation.Nonnull;

@FunctionalInterface
public interface ContextSupplier {

  @Nonnull
  Context supply();
}
