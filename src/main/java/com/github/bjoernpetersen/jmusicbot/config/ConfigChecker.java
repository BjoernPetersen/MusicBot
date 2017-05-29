package com.github.bjoernpetersen.jmusicbot.config;

import java.util.Optional;
import javax.annotation.Nonnull;

@FunctionalInterface
public interface ConfigChecker {

  @Nonnull
  Optional<String> check(@Nonnull String value);
}
