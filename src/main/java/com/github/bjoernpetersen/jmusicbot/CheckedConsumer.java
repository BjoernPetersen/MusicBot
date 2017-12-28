package com.github.bjoernpetersen.jmusicbot;

import java.util.Objects;

@FunctionalInterface
public interface CheckedConsumer<T, E extends Exception>{
  void accept(T arg1) throws E;

  default CheckedConsumer<T, E> andThen(CheckedConsumer<? super T, E> var1) {
    Objects.requireNonNull(var1);
    return (var2) -> {
      this.accept(var2);
      var1.accept(var2);
    };
  }
}
