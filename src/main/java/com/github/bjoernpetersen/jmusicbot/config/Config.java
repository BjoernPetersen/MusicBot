package com.github.bjoernpetersen.jmusicbot.config;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class Config {

  @Nonnull
  private static final Logger log = Logger.getLogger(Config.class.getName());

  @Nonnull
  private final ConfigStorageAdapter adapter;
  @Nonnull
  private final Map<String, String> config;
  @Nonnull
  private final Map<String, String> secrets;

  @Nonnull
  private final Map<String, WeakReference<Entry>> weakEntries;
  @Nonnull
  private final Map<String, Entry> entries;

  public Config(@Nonnull ConfigStorageAdapter adapter) {
    this.adapter = adapter;
    this.config = new HashMap<>(adapter.loadPlaintext());
    this.secrets = new HashMap<>(adapter.loadSecrets());

    this.weakEntries = new HashMap<>();
    this.entries = new HashMap<>();

    DefaultConfigEntry.get(this);
  }

  @Nullable
  private String getValue(String key) {
    return config.get(key);
  }

  private void setValue(String key, @Nullable String value) {
    String old = getValue(key);
    if (!Objects.equals(old, value)) {
      log.finer(String.format("Config entry '%s' changed from '%s' to '%s'",
          key,
          getValue(key),
          value)
      );
      config.put(key, value);
      adapter.storePlaintext(config);
    }
  }

  @Nullable
  private String getSecret(String key) {
    return secrets.get(key);
  }

  private void setSecret(String key, @Nullable String value) {
    String old = getSecret(key);
    if (!Objects.equals(old, value)) {
      log.finer(String.format("Secret '%s' changed.", key));
      secrets.put(key, value);
      adapter.storeSecrets(secrets);
    }
  }

  @Nullable
  private Entry getEntry(String key) {
    Entry entry = entries.get(key);
    if (entry != null) {
      return entry;
    }

    WeakReference<Entry> weakEntry = weakEntries.get(key);
    if (weakEntry != null) {
      entry = weakEntry.get();
      if (entry != null) {
        entry = null;
        System.gc();
      }
      entry = weakEntry.get();
      weakEntries.remove(key);
      if (entry != null) {
        entries.put(key, entry);
        return entry;
      }
    }

    return null;
  }

  /**
   * Gets a string config entry. The entry has to be defined by calling {@link #stringEntry(Class,
   * String, String, String)} prior to this method.
   *
   * @param type the class that defined the entry
   * @param key the entry key
   * @return a string config entry
   * @throws IllegalArgumentException if there is no such entry or it is secret
   */
  @Nonnull
  public ReadOnlyStringEntry stringEntry(Class<?> type, String key) {
    Entry entry = getEntry(key);
    if (entry instanceof ReadOnlyStringEntry && !entry.isSecret()) {
      return (ReadOnlyStringEntry) entry;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Defines and returns a string config entry. If it has already been defined, no new instance will
   * be created.
   *
   * @param type the class defining the entry. It's fully qualified name is prepended to the key for
   * uniqueness
   * @param key the entry key
   * @param description a description for the user what this entry does
   * @param defaultValue a default value, or null
   * @return a string config entry
   * @throws IllegalArgumentException if the entry has already been defined, but is secret or a
   * BooleanEntry
   */
  @Nonnull
  public StringEntry stringEntry(Class<?> type, String key, String description,
      @Nullable String defaultValue) {
    key = type.getName() + "." + key;
    Entry entry = getEntry(key);
    if (entry != null) {
      if (entry instanceof StringEntry && !entry.isSecret()) {
        return (StringEntry) entry;
      } else {
        throw new IllegalArgumentException();
      }
    }
    return new StringEntry(key, description, defaultValue, false);
  }

  /**
   * Gets a string config entry with a secret. The entry has to be defined by calling {@link
   * #secret(Class, String, String)} prior to this method.
   *
   * @param type the class that defined the entry
   * @param key the entry key
   * @return a string config entry
   * @throws IllegalArgumentException if there is no such entry or it is not a secret
   */
  @Nonnull
  public ReadOnlyStringEntry secret(Class<?> type, String key) {
    key = type.getName() + "." + key;
    Entry entry = getEntry(key);
    if (entry instanceof ReadOnlyStringEntry && entry.isSecret()) {
      return (ReadOnlyStringEntry) entry;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * <p>Defines and returns a string config entry holding a secret. If it has already been defined,
   * no new instance will be created.</p>
   *
   * <p>Secret string entries are stored separately from plaintext string entries. They should also
   * not be visible to the user.</p>
   *
   * @param type the class defining the entry. It's fully qualified name is prepended to the key for
   * uniqueness
   * @param key the entry key
   * @param description a description for the user what this entry does
   * @return a string config entry
   * @throws IllegalArgumentException if the entry has already been defined, but is not secret or it
   * is a BooleanEntry
   */
  @Nonnull
  public StringEntry secret(Class<?> type, String key, String description) {
    key = type.getName() + "." + key;
    Entry entry = getEntry(key);
    if (entry != null) {
      if (entry instanceof StringEntry && entry.isSecret()) {
        return (StringEntry) entry;
      } else {
        throw new IllegalArgumentException();
      }
    }
    return new StringEntry(key, description, null, true);
  }

  /**
   * Gets a boolean config entry. The entry has to be defined by calling {@link #booleanEntry(Class,
   * String, String, boolean)} prior to this method.
   *
   * @param type the class that defined the entry
   * @param key the entry key
   * @return a boolean config entry
   * @throws IllegalArgumentException if there is no such entry or it is a StringEntry
   */
  @Nonnull
  public ReadOnlyBooleanEntry booleanEntry(Class<?> type, String key) {
    key = type.getName() + "." + key;
    Entry entry = getEntry(key);
    if (entry instanceof ReadOnlyBooleanEntry && !entry.isSecret()) {
      return (ReadOnlyBooleanEntry) entry;
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Defines and returns a boolean config entry. If it has already been defined, no new instance
   * will be created.
   *
   * @param type the class defining the entry. It's fully qualified name is prepended to the key for
   * uniqueness
   * @param key the entry key
   * @param description a description for the user what this entry does
   * @param defaultValue a default value
   * @return a boolean config entry
   * @throws IllegalArgumentException if the entry has already been defined, but is a StringEntry
   */
  @Nonnull
  public BooleanEntry booleanEntry(Class<?> type, String key, String description,
      boolean defaultValue) {
    key = type.getName() + "." + key;
    Entry entry = getEntry(key);
    if (entry != null) {
      if (entry instanceof BooleanEntry && !entry.isSecret()) {
        return (BooleanEntry) entry;
      } else {
        throw new IllegalArgumentException();
      }
    }
    return new BooleanEntry(key, description, defaultValue);
  }

  /**
   * <p>A config entry, holding it's key and description.</p>
   *
   * There are two implementations of Config.Entry:<br>
   * <ul>
   * <li>{@link StringEntry} for string entries and secrets</li>
   * <li>{@link BooleanEntry} for boolean entries</li>
   * </ul>
   */
  public abstract class Entry {

    private Entry(@Nonnull String key, @Nonnull String description, boolean isSecret) {
      this.key = key;
      this.description = description;
      this.isSecret = isSecret;
      Config.this.entries.put(key, this);
    }

    @Nonnull
    private final String key;
    @Nonnull
    private final String description;
    private final boolean isSecret;

    @Nonnull
    public String getKey() {
      return key;
    }

    @Nonnull
    public String getDescription() {
      return description;
    }

    public boolean isSecret() {
      return isSecret;
    }

    void set(@Nullable String value) {
      if (isSecret()) {
        Config.this.setSecret(getKey(), value);
      } else {
        Config.this.setValue(getKey(), value);
      }
    }

    /**
     * Schedules this entry for destruction. After calling this method, any references to it should
     * be set to <code>null</code>.
     */
    public void tryDestruct() {
      WeakReference<Entry> ref = new WeakReference<>(this);
      entries.remove(getKey());
      weakEntries.put(getKey(), ref);
    }
  }

  /**
   * <p>Read-only implementation of Entry supporting string values.</p>
   *
   * <p>This implementation provides various possibilities to access the entry value and/or the
   * default value.</p>
   */
  @ParametersAreNonnullByDefault
  public class ReadOnlyStringEntry extends Entry {

    @Nullable
    private final String defaultValue;
    @Nonnull
    private final Set<StringConfigListener> listeners;

    private ReadOnlyStringEntry(String key,
        String description,
        @Nullable String defaultValue,
        boolean isSecret) {
      super(key, description, isSecret);
      this.defaultValue = defaultValue;
      this.listeners = new HashSet<>();
    }

    public void addListener(StringConfigListener listener) {
      listeners.add(listener);
    }

    public void removeListener(StringConfigListener listener) {
      listeners.remove(listener);
    }

    @Override
    void set(@Nullable String value) {
      String oldValue = get().orElse(null);
      super.set(value);
      if (!Objects.equals(oldValue, value)) {
        listeners.forEach(c -> c.onChange(oldValue, value));
      }
    }

    /**
     * Gets the value of the entry. If a default value is present, it is ignored.
     *
     * @return optional of the entry value
     */
    @Nonnull
    public Optional<String> get() {
      Config config = Config.this;
      String value = isSecret() ? config.getSecret(getKey()) : config.getValue(getKey());
      return Optional.ofNullable(value);
    }

    /**
     * Gets the value of the entry. If there is no value, but a default value, it will be present.
     * If there is neither, the returned Optional will be empty.
     *
     * @return optional of the entry value or the default value, if present
     */
    @Nonnull
    public Optional<String> getWithDefault() {
      Optional<String> value = get();
      if (!value.isPresent() && hasDefault()) {
        return Optional.of(defaultValue);
      } else {
        return value;
      }
    }

    /**
     * Gets the default value, if one is present.
     *
     * @return optional of the default value
     */
    @Nonnull
    public Optional<String> getDefault() {
      return Optional.ofNullable(defaultValue);
    }

    /**
     * Whether a default value is present.
     *
     * @return true, if there is a default value
     */
    public boolean hasDefault() {
      return defaultValue != null;
    }

    /**
     * <p>Gets the entry value. If no value is present, the default value is returned.</p>
     *
     * <p>This method should only be called if a default value is present. Otherwise, an exception
     * is thrown.</p>
     *
     * @return the entry value or default value
     * @throws IllegalStateException if this method is called and there is no default value
     */
    @Nonnull
    public String getOrDefault() {
      if (defaultValue == null) {
        throw new IllegalStateException();
      }
      return get().orElse(defaultValue);
    }
  }

  /**
   * An extension of {@link ReadOnlyStringEntry} providing a {@link #set(String)} method.
   */
  public class StringEntry extends ReadOnlyStringEntry {

    private StringEntry(String key, String description, @Nullable String defaultValue,
        boolean isSecret) {
      super(key, description, defaultValue, isSecret);
    }

    /**
     * <p>Changes the entry value to the specified new one.</p>
     *
     * <p>The new value will immediately be persistently stored.</p>
     *
     * @param value the new value, or null
     */
    @Override
    public void set(@Nullable String value) {
      super.set(value);
    }
  }

  /**
   * <p>Read-only implementation of Entry supporting boolean values.</p>
   *
   * <p>This implementation must have a default value.</p>
   *
   * <p>This implementation provides a {@link #get()} method to access the current value.</p>
   */
  public class ReadOnlyBooleanEntry extends Entry {

    private final boolean defaultValue;
    private final Set<BooleanConfigListener> listeners;

    private ReadOnlyBooleanEntry(String key,
        String description,
        boolean defaultValue) {
      super(key, description, false);
      this.defaultValue = defaultValue;
      this.listeners = new HashSet<>();
    }


    public void addListener(BooleanConfigListener listener) {
      listeners.add(listener);
    }

    public void removeListener(BooleanConfigListener listener) {
      listeners.remove(listener);
    }

    @Override
    void set(@Nullable String value) {
      boolean oldValue = get();
      super.set(value);
      boolean newValue = get();
      if (!Objects.equals(oldValue, newValue)) {
        listeners.forEach(c -> c.onChange(oldValue, newValue));
      }
    }

    /**
     * Gets the current entry value. If no current value is present, the default value is returned.
     *
     * @return the entry value
     */
    public boolean get() {
      Config config = Config.this;
      String value = isSecret() ? config.getSecret(getKey()) : config.getValue(getKey());
      if (value == null || value.trim().equals("")) {
        log.finest("Returning default for missing or empty value: " + getKey());
        return defaultValue;
      } else {
        log.finest("Getting bool from string: '" + value + "'");
        return Boolean.parseBoolean(value);
      }
    }
  }

  /**
   * An extension of {@link ReadOnlyBooleanEntry} providing a {@link #set(boolean)} method.
   */
  public class BooleanEntry extends ReadOnlyBooleanEntry {

    private BooleanEntry(String key, String description, boolean defaultValue) {
      super(key, description, defaultValue);
    }

    /**
     * <p>Sets the entry value to the specified new one.</p>
     *
     * <p>The new value will immediately be persistently stored.</p>
     *
     * @param value the new value
     */
    public void set(boolean value) {
      set(Boolean.toString(value));
    }
  }
}
