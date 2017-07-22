package com.github.bjoernpetersen.jmusicbot;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A simple, mutable Reference.
 *
 * @param <T> the type of the referenced object.
 */
public final class Reference<T> {

  private T object;

  public Reference() {
  }

  public Reference(@Nonnull T object) {
    this.object = object;
  }

  public T get() {
    return object;
  }

  public void set(@Nullable T object) {
    this.object = object;
  }
}
