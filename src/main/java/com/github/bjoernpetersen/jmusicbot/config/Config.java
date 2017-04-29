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

  private final Map<String, WeakReference<Entry>> weakEntries;
  @Nonnull
  private final Map<String, Entry> entries;

  public Config(ConfigStorageAdapter adapter) {
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

  @Nonnull
  public ReadOnlyStringEntry stringEntry(Class<?> type, String key) {
    Entry entry = getEntry(key);
    if (entry instanceof ReadOnlyStringEntry && !entry.isSecret()) {
      return (ReadOnlyStringEntry) entry;
    } else {
      throw new IllegalArgumentException();
    }
  }

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

    public void tryDestruct() {
      WeakReference<Entry> ref = new WeakReference<>(this);
      entries.remove(getKey());
      weakEntries.put(getKey(), ref);
    }
  }

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

    @Nonnull
    public Optional<String> get() {
      Config config = Config.this;
      String value = isSecret() ? config.getSecret(getKey()) : config.getValue(getKey());
      return Optional.ofNullable(value);
    }

    @Nonnull
    public Optional<String> getWithDefault() {
      Optional<String> value = get();
      if (!value.isPresent() && hasDefault()) {
        return Optional.of(defaultValue);
      } else {
        return value;
      }
    }

    @Nullable
    public String getDefault() {
      return defaultValue;
    }

    public boolean hasDefault() {
      return defaultValue != null;
    }

    @Nonnull
    public String getOrDefault() {
      if (defaultValue == null) {
        throw new IllegalStateException();
      }
      return get().orElse(defaultValue);
    }
  }

  public class StringEntry extends ReadOnlyStringEntry {

    private StringEntry(String key, String description, @Nullable String defaultValue,
      boolean isSecret) {
      super(key, description, defaultValue, isSecret);
    }

    @Override
    public void set(@Nullable String value) {
      super.set(value);
    }
  }

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

  public class BooleanEntry extends ReadOnlyBooleanEntry {

    private BooleanEntry(String key, String description, boolean defaultValue) {
      super(key, description, defaultValue);
    }

    public void set(boolean value) {
      set(Boolean.toString(value));
    }
  }
}
