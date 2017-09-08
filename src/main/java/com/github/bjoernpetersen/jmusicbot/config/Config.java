package com.github.bjoernpetersen.jmusicbot.config;

import com.github.bjoernpetersen.jmusicbot.Loggable;
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

public final class Config implements Loggable {

  @Nonnull
  private final Logger logger;

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
    this.logger = createLogger();
    this.adapter = adapter;
    this.config = new HashMap<>(adapter.loadPlaintext());
    this.secrets = new HashMap<>(adapter.loadSecrets());

    this.weakEntries = new HashMap<>();
    this.entries = new HashMap<>();

    DefaultConfigEntry.get(this);
  }

  @Override
  @Nonnull
  public Logger getLogger() {
    return logger;
  }

  @Nullable
  private String getValue(@Nonnull String key) {
    return config.get(key);
  }

  private void setValue(@Nonnull String key, @Nullable String value) {
    String old = getValue(key);
    if (!Objects.equals(old, value)) {
      logFiner("Config entry '%s' changed from '%s' to '%s'", key, getValue(key), value);
      if (value == null) {
        config.remove(key);
      } else {
        config.put(key, value);
      }
      adapter.storePlaintext(config);
    }
  }

  @Nullable
  private String getSecret(@Nonnull String key) {
    return secrets.get(key);
  }

  private void setSecret(@Nonnull String key, @Nullable String value) {
    String old = getSecret(key);
    if (!Objects.equals(old, value)) {
      logFiner("Secret '%s' changed.", key);
      if (value == null) {
        secrets.remove(key);
      } else {
        secrets.put(key, value);
      }
      adapter.storeSecrets(secrets);
    }
  }

  @Nullable
  private Entry getEntry(@Nonnull String key) {
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
   * String, String, String, ConfigChecker)} prior to this method.
   *
   * @param type the class that defined the entry
   * @param key the entry key
   * @return a string config entry
   * @throws IllegalArgumentException if there is no such entry or it is secret
   */
  @Nonnull
  public ReadOnlyStringEntry stringEntry(@Nonnull Class<?> type, @Nonnull String key) {
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
   * @param errorChecker an error checker
   * @return a string config entry
   * @throws IllegalArgumentException if the entry has already been defined, but is secret or a
   * BooleanEntry
   */
  @Nonnull
  public StringEntry stringEntry(@Nonnull Class<?> type, @Nonnull String key,
      @Nonnull String description, @Nullable String defaultValue,
      @Nonnull ConfigChecker errorChecker) {
    key = type.getName() + "." + key;
    Entry entry = getEntry(key);
    if (entry != null) {
      if (entry instanceof StringEntry && !entry.isSecret()) {
        return (StringEntry) entry;
      } else {
        throw new IllegalArgumentException();
      }
    }
    return new StringEntry(key, description, defaultValue, false, errorChecker);
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
  public ReadOnlyStringEntry secret(@Nonnull Class<?> type, @Nonnull String key) {
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
  public StringEntry secret(@Nonnull Class<?> type, @Nonnull String key,
      @Nonnull String description) {
    key = type.getName() + "." + key;
    Entry entry = getEntry(key);
    if (entry != null) {
      if (entry instanceof StringEntry && entry.isSecret()) {
        return (StringEntry) entry;
      } else {
        throw new IllegalArgumentException();
      }
    }
    return new StringEntry(key, description, null, true, v -> null);
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
  public ReadOnlyBooleanEntry booleanEntry(@Nonnull Class<?> type, @Nonnull String key) {
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
  public BooleanEntry booleanEntry(@Nonnull Class<?> type, @Nonnull String key,
      @Nonnull String description, boolean defaultValue) {
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
   * There are two implementations of Config.Entry:<br> <ul> <li>{@link StringEntry} for string
   * entries and secrets</li> <li>{@link BooleanEntry} for boolean entries</li> </ul>
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
      if (value != null && value.isEmpty()) {
        logFinest("Replacing empty new value with null");
        value = null;
      }
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
  public class ReadOnlyStringEntry extends Entry {

    @Nullable
    private final String defaultValue;
    @Nonnull
    private final Set<StringConfigListener> listeners;
    @Nonnull
    private final ConfigChecker checker;

    private ReadOnlyStringEntry(@Nonnull String key,
        @Nonnull String description,
        @Nullable String defaultValue,
        boolean isSecret,
        @Nonnull ConfigChecker checker) {
      super(key, description, isSecret);
      this.defaultValue = defaultValue;
      this.listeners = new HashSet<>();
      this.checker = checker;
    }

    public void addListener(@Nonnull StringConfigListener listener) {
      listeners.add(listener);
    }

    public void removeListener(@Nonnull StringConfigListener listener) {
      listeners.remove(listener);
    }

    @Override
    void set(@Nullable String value) {
      String oldValue = get().orElse(null);
      if (!Objects.equals(oldValue, value)) {
        super.set(value);
        listeners.forEach(c -> c.onChange(oldValue, value));
      }
    }

    @Nonnull
    public Optional<String> checkError() {
      return get().flatMap(value -> Optional.ofNullable(checker.check(value)));
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
      if (value != null && value.isEmpty()) {
        logFinest("Replacing empty value with null");
        value = null;
      }
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

    private StringEntry(@Nonnull String key, @Nonnull String description,
        @Nullable String defaultValue, boolean isSecret, @Nonnull ConfigChecker checker) {
      super(key, description, defaultValue, isSecret, checker);
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
   *
   * <p><b>Note:</b> a BooleanEntry will actually be stored as a string. {@link
   * Boolean#parseBoolean(String)} will be used to parse a value from the config, which means
   * <code>false</code> will be parsed if the value is anything other than "true".</p>
   */
  public class ReadOnlyBooleanEntry extends Entry {

    private final boolean defaultValue;
    private final Set<BooleanConfigListener> listeners;

    private ReadOnlyBooleanEntry(@Nonnull String key,
        @Nonnull String description,
        boolean defaultValue) {
      super(key, description, false);
      this.defaultValue = defaultValue;
      this.listeners = new HashSet<>();
    }

    public void addListener(@Nonnull BooleanConfigListener listener) {
      listeners.add(listener);
    }

    public void removeListener(@Nonnull BooleanConfigListener listener) {
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
        logFinest("Returning default for missing or empty value: " + getKey());
        return defaultValue;
      } else {
        logFinest("Getting bool from string: '" + value + "'");
        return Boolean.parseBoolean(value);
      }
    }
  }

  /**
   * An extension of {@link ReadOnlyBooleanEntry} providing a {@link #set(boolean)} method.
   */
  public class BooleanEntry extends ReadOnlyBooleanEntry {

    private BooleanEntry(@Nonnull String key, @Nonnull String description, boolean defaultValue) {
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
