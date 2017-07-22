package com.github.bjoernpetersen.jmusicbot.provider;

import com.github.bjoernpetersen.jmusicbot.Plugin;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class DependencyMap<T extends Plugin> {

  @Nonnull
  private final Map<Class<? extends T>, T> wrapped;

  public DependencyMap(@Nonnull Map<Class<? extends T>, T> wrapped) {
    this.wrapped = wrapped;
  }

  @Nullable
  public <Base extends T> Base get(Class<Base> baseClass) {
    return (Base) wrapped.get(baseClass);
  }
}
