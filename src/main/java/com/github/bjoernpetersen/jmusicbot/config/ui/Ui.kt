package com.github.bjoernpetersen.jmusicbot.config.ui

import com.github.bjoernpetersen.jmusicbot.config.Config

interface ConfigValueConverter<in T : Config.Entry, U> {
  fun getWithoutDefault(t: T): U
  fun getDefault(t: T): U
  fun set(t: T, u: U)
}

object DefaultBooleanConverter : ConfigValueConverter<Config.BooleanEntry, Boolean> {
  override fun getDefault(t: Config.BooleanEntry): Boolean = t.defaultValue
  override fun getWithoutDefault(t: Config.BooleanEntry): Boolean = t.value
  override fun set(t: Config.BooleanEntry, u: Boolean) = t.set(u)
}

object DefaultStringConverter : ConfigValueConverter<Config.StringEntry, String?> {
  override fun getDefault(t: Config.StringEntry): String? = t.defaultValue
  override fun getWithoutDefault(t: Config.StringEntry): String? = t.valueWithoutDefault
  override fun set(t: Config.StringEntry, u: String?) = t.set(u)
}

sealed class UiNode<in T : Config.Entry, U>(val converter: ConfigValueConverter<T, U>)
class TextBox<in T : Config.Entry>(converter: ConfigValueConverter<T, String?>) :
    UiNode<T, String?>(converter)

class PasswordBox<in T : Config.Entry>(converter: ConfigValueConverter<T, String?>) :
    UiNode<T, String?>(converter)

class NumberBox<in T : Config.Entry>(converter: ConfigValueConverter<T, Int?>,
    val min: Int = 0, val max: Int = 100) : UiNode<T, Int?>(converter)

class CheckBox<in T : Config.Entry>(converter: ConfigValueConverter<T, Boolean>) :
    UiNode<T, Boolean>(converter)

class ChoiceBox<in T : Config.Entry>(converter: ConfigValueConverter<T, String?>,
    val refresh: () -> List<String>?) : UiNode<T, String?>(converter)
// TODO: file chooser button
