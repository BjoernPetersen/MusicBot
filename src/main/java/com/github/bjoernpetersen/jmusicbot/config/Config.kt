package com.github.bjoernpetersen.jmusicbot.config

import com.github.bjoernpetersen.jmusicbot.Loggable
import com.github.bjoernpetersen.jmusicbot.MusicBot
import com.github.bjoernpetersen.jmusicbot.config.ui.CheckBox
import com.github.bjoernpetersen.jmusicbot.config.ui.PasswordBox
import com.github.bjoernpetersen.jmusicbot.config.ui.TextBox
import com.github.bjoernpetersen.jmusicbot.config.ui.UiNode
import com.github.bjoernpetersen.jmusicbot.platform.HostServices
import java.util.*
import java.util.logging.Logger

interface Defaults {
  val pluginFolder: Config.StringEntry
  val entries: List<Config.Entry>
    get() = listOf(pluginFolder)
}

class Config(private val adapter: ConfigStorageAdapter, val hostServices: HostServices) : Loggable {
  private val logger: Logger = createLogger()
  private val config: MutableMap<String, String>
  private val secrets: MutableMap<String, String>

  private val entries: MutableMap<String, Entry>

  init {
    this.config = HashMap(adapter.loadPlaintext())
    this.secrets = HashMap(adapter.loadSecrets())

    this.entries = HashMap()
  }

  override fun getLogger(): Logger {
    return logger
  }

  private fun qualify(base: Class<*>, key: String): String = base.name + "." + key

  private fun getValue(base: Class<*>, key: String): String? {
    return config[qualify(base, key)]
  }

  private fun setValue(base: Class<*>, key: String, value: String?) {
    val old = getValue(base, key)
    if (old != value) {
      val qualified = qualify(base, key)
      logFiner("Config entry '%s' changed from '%s' to '%s'", qualified, getValue(base, key), value)
      if (value == null) {
        config.remove(qualified)
      } else {
        config.put(qualified, value)
      }
      adapter.storePlaintext(config)
    }
  }

  private fun getSecret(base: Class<*>, key: String): String? {
    return secrets[qualify(base, key)]
  }

  private fun setSecret(base: Class<*>, key: String, value: String?) {
    val old = getSecret(base, key)
    if (old != value) {
      val qualified = qualify(base, key)
      logFiner("Secret '%s' changed.", qualified)
      if (value == null) {
        secrets.remove(qualified)
      } else {
        secrets.put(qualified, value)
      }
      adapter.storeSecrets(secrets)
    }
  }

  private fun addEntry(base: Class<*>, key: String, entry: Entry) {
    entries.put(qualify(base, key), entry)
  }

  private fun removeEntry(base: Class<*>, key: String) {
    entries.remove(qualify(base, key))
  }

  private fun getEntry(base: Class<*>, key: String): Entry? {
    return entries[qualify(base, key)]
  }

  private fun checkExists(type: Class<*>, key: String): Unit {
    if (getEntry(type, key) != null) throw IllegalArgumentException()
  }

  /**
   *
   * A config entry, holding it's key and description.
   *
   * There are two implementations of Config.Entry:<br></br>   * [StringEntry] for string
   * entries and secrets  * [BooleanEntry] for boolean entries
   */
  abstract inner class Entry protected constructor(
      val base: Class<*>,
      val key: String,
      val description: String,
      val isSecret: Boolean,
      val ui: UiNode<*, *, *>) {

    init {
      checkExists(base, key)
      addEntry(base, key, this)
    }

    protected open fun set(value: String?) {
      val actual = if (value != null && value.trim().isEmpty()) {
        logFinest("Replacing empty new value with null")
        null
      } else value
      if (isSecret) {
        setSecret(base, key, actual)
      } else {
        setValue(base, key, actual)
      }
    }

    fun destruct() {
      removeEntry(base, key)
    }
  }

  /**
   *
   * Read-only implementation of Entry supporting string values.
   *
   *
   * This implementation provides various possibilities to access the entry value and/or the
   * default value.
   */
  abstract inner class ReadOnlyStringEntry internal constructor(
      base: Class<*>,
      key: String,
      description: String,
      isSecret: Boolean,
      ui: UiNode<StringEntry, *, *>,
      val defaultValue: String?,
      private val checker: ConfigChecker) : Entry(base, key, description, isSecret, ui) {

    private val listeners: MutableSet<ConfigListener<String?>>

    init {
      this.listeners = HashSet()
    }

    fun addListener(listener: ConfigListener<String?>) {
      listeners.add(listener)
    }

    fun removeListener(listener: ConfigListener<String?>) {
      listeners.remove(listener)
    }

    override fun set(value: String?) {
      val oldValue = valueWithoutDefault
      if (oldValue != value) {
        super.set(value)
        listeners.forEach { c -> c.onChange(oldValue, value) }
      }
    }

    fun checkError(): String? {
      return valueWithoutDefault?.let { checker.check(it) }
    }

    /**
     * Gets the value of the entry. If a default value is present, it is ignored.
     *
     * @return optional of the entry value
     */
    val valueWithoutDefault: String?
      get() {
        val config = this@Config
        var value: String? = if (isSecret) config.getSecret(base, key) else config.getValue(base,
            key)
        if (value != null && value.isEmpty()) {
          logFinest("Replacing empty value with null")
          value = null
        }
        return value
      }

    /**
     * Gets the value of the entry. If there is no value, but a default value, it will be present.
     * If there is neither, null will be returned.
     *
     * @return the entry value or the default value, or null if there is no default
     */
    val value: String?
      get() {
        return valueWithoutDefault ?: defaultValue
      }
  }

  /**
   * An extension of [ReadOnlyStringEntry] providing a [set] method.
   *
   * Secret string entries are stored separately from plaintext string entries. They should also
   * not be visible to the user.
   *
   * @param base the class defining the entry. Its fully qualified name is prepended to the key for
   * uniqueness
   * @param key the entry key
   * @param description a description for the user what this entry does
   * @param isSecret whether this entry is a secret
   * @param default a default value. Must not be present if this entry is secret
   * @param ui the UI node to show in the configuration window
   * @param checker a config checker
   * @return a string config entry
   * @throws IllegalArgumentException if the entry has already been defined
   */
  inner class StringEntry @JvmOverloads constructor(
      base: Class<*>,
      key: String,
      description: String,
      isSecret: Boolean,
      default: String? = null,
      ui: UiNode<StringEntry, *, *> = if (isSecret) PasswordBox() else TextBox(),
      checker: ConfigChecker = ConfigChecker { null }) :
      ReadOnlyStringEntry(base, key, description, isSecret, ui, default, checker) {

    init {
      if (isSecret && default != null) {
        throw IllegalArgumentException("Secret entries can't have default values")
      }
    }

    /**
     *
     * Changes the entry value to the specified new one.
     *
     *
     * The new value will immediately be persistently stored.
     *
     * @param value the new value, or null
     */
    public override fun set(value: String?) {
      super.set(value)
    }
  }

  /**
   *
   * Read-only implementation of Entry supporting boolean values.
   *
   *
   * This implementation must have a default value.
   *
   *
   * This implementation provides a [.get] method to access the current value.
   *
   *
   * **Note:** a BooleanEntry will actually be stored as a string. [ ][Boolean.parseBoolean] will be used to parse a value from the config, which means
   * `false` will be parsed if the value is anything other than "true".
   */
  abstract inner class ReadOnlyBooleanEntry internal constructor(
      base: Class<*>,
      key: String,
      description: String,
      val defaultValue: Boolean,
      ui: UiNode<BooleanEntry, *, *>) : Entry(base, key, description, false, ui) {

    private val listeners: MutableSet<ConfigListener<Boolean>>

    init {
      this.listeners = HashSet()
    }

    fun addListener(listener: ConfigListener<Boolean>) {
      listeners.add(listener)
    }

    fun removeListener(listener: ConfigListener<Boolean>) {
      listeners.remove(listener)
    }

    override fun set(value: String?) {
      val oldValue = this.value
      super.set(value)
      val newValue = this.value
      if (oldValue != newValue) {
        listeners.forEach { c -> c.onChange(oldValue, newValue) }
      }
    }

    /**
     * Gets the current entry value. If no current value is present, the default value is returned.
     *
     * @return the entry value
     */
    val value: Boolean
      get() {
        val config = this@Config
        val value = if (isSecret) config.getSecret(base, key) else config.getValue(base, key)
        return if (value == null || value.trim { it <= ' ' } == "") {
          logFinest("Returning default for missing or empty value: $key")
          defaultValue
        } else {
          logFinest("Getting bool from string: '$value'")
          value.toBoolean()
        }
      }
  }


  /**
   * An extension of [ReadOnlyBooleanEntry] providing a [set] method.
   *
   * Defines and returns a new boolean config entry.
   *
   * @param base the class defining the entry. Its fully qualified name is prepended to the key for
   * uniqueness
   * @param key the entry key
   * @param description a description for the user what this entry does
   * @param defaultValue a default value
   * @param ui a UI node to show in the configuration window
   * @return a boolean config entry
   * @throws IllegalArgumentException if the entry has already been defined, but is a StringEntry
   */
  inner class BooleanEntry @JvmOverloads constructor(
      base: Class<*>,
      key: String,
      description: String,
      defaultValue: Boolean,
      ui: UiNode<BooleanEntry, *, *> = CheckBox()) :
      ReadOnlyBooleanEntry(base, key, description, defaultValue, ui) {

    /**
     * Sets the entry value to the specified new one.
     *
     * The new value will immediately be persistently stored.
     *
     * @param value the new value
     */
    fun set(value: Boolean?) {
      super.set(value?.toString())
    }
  }

  val defaults = object : Defaults {
    override val pluginFolder = StringEntry(
        MusicBot::class.java,
        "pluginFolder",
        "This is where the application looks for plugin files",
        false,
        "plugins"
    )
  }
}

@FunctionalInterface
interface ConfigListener<T> {

  fun onChange(oldValue: T, newValue: T)
}
