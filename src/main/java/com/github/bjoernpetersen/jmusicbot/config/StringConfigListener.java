package com.github.bjoernpetersen.jmusicbot.config;

import javax.annotation.Nullable;

@Deprecated
@FunctionalInterface
public interface StringConfigListener extends ConfigListener<String> {

  void onChange(@Nullable String before, @Nullable String after);
}
