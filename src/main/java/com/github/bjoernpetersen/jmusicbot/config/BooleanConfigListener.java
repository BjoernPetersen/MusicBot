package com.github.bjoernpetersen.jmusicbot.config;

@FunctionalInterface
public interface BooleanConfigListener {

  void onChange(boolean before, boolean after);
}
