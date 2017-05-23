package com.github.bjoernpetersen.jmusicbot;

import javax.annotation.Nonnull;

public interface Testable<T> {

  @Nonnull
  T createValue();
}
