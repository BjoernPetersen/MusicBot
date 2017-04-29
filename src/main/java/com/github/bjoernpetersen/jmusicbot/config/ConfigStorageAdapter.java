package com.github.bjoernpetersen.jmusicbot.config;

import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface ConfigStorageAdapter {

  @Nonnull
  Map<String, String> loadPlaintext();

  void storePlaintext(Map<String, String> plain);

  @Nonnull
  Map<String, String> loadSecrets();

  void storeSecrets(Map<String, String> secrets);
}
