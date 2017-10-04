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
class TextBox() : UiNode<Config.StringEntry, String?>(DefaultStringConverter)
class PasswordBox() : UiNode<Config.StringEntry, String?>(DefaultStringConverter)
class CheckBox() : UiNode<Config.BooleanEntry, Boolean>(DefaultBooleanConverter)
class NumberBox(val min: Int = 0, val max: Int = 100) :
    UiNode<Config.StringEntry, Int>(object : ConfigValueConverter<Config.StringEntry, Int> {
      override fun getDefault(t: Config.StringEntry): Int = try {
        t.defaultValue?.toInt() ?: min
      } catch (e: NumberFormatException) {
        min
      }

      override fun getWithoutDefault(t: Config.StringEntry): Int = try {
        t.valueWithoutDefault?.toInt() ?: getDefault(t)
      } catch (e: NumberFormatException) {
        getDefault(t)
      }

      override fun set(t: Config.StringEntry, u: Int) = t.set(u.toString())
    })

class ChoiceBox(val refresh: () -> List<String>?,
    converter: ConfigValueConverter<Config.StringEntry, String?> = DefaultStringConverter) :
    UiNode<Config.StringEntry, String?>(converter)

class FileChooserButton(val isFolder: Boolean = true,
    converter: ConfigValueConverter<Config.StringEntry, String?> = DefaultStringConverter) :
    UiNode<Config.StringEntry, String?>(converter)
