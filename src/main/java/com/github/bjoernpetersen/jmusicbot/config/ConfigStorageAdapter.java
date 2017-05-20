package com.github.bjoernpetersen.jmusicbot.config;

import java.util.Map;
import javax.annotation.Nonnull;

/**
 * An adapter for loading and storing config entries and secrets.
 */
public interface ConfigStorageAdapter {

  @Nonnull
  Map<String, String> loadPlaintext();

  /**
   * <p>Persistently stores config entries. These can be stored in plaintext.</p>
   *
   * <p>This method is likely to be called often, so it should be fast and should not need user
   * interaction.</p>
   *
   * @param plain a map of config entries
   */
  void storePlaintext(@Nonnull Map<String, String> plain);

  @Nonnull
  Map<String, String> loadSecrets();

  /**
   * <p>Stores the secrets in a secure manner.</p>
   *
   * <p>This method is likely to be called often, so it should be fast and should not need user
   * interaction.</p>
   *
   * @param secrets a map of config entries with secret values
   */
  void storeSecrets(@Nonnull Map<String, String> secrets);
}
