package com.github.bjoernpetersen.jmusicbot.config;

import javax.annotation.Nullable;

@FunctionalInterface
public interface StringConfigListener {

  void onChange(@Nullable String before, @Nullable String after);
}
