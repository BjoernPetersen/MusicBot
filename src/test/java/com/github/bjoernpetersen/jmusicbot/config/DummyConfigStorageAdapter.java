package com.github.bjoernpetersen.jmusicbot.config;

import java.util.Collections;
import java.util.Map;
import javax.annotation.Nonnull;

public class DummyConfigStorageAdapter implements ConfigStorageAdapter {

  @Nonnull
  @Override
  public Map<String, String> loadPlaintext() {
    return Collections.emptyMap();
  }

  @Override
  public void storePlaintext(@Nonnull Map<String, String> plain) {
  }

  @Nonnull
  @Override
  public Map<String, String> loadSecrets() {
    return Collections.emptyMap();
  }

  @Override
  public void storeSecrets(@Nonnull Map<String, String> secrets) {
  }
}
