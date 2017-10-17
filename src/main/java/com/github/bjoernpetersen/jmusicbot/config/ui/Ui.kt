package com.github.bjoernpetersen.jmusicbot.config.ui

import com.github.bjoernpetersen.jmusicbot.config.Config

interface ConfigValueConverter<in T : Config.Entry, in S, out G> {
  fun getWithoutDefault(t: T): G
  fun getDefault(t: T): G
  fun getWithDefault(t: T): G
  fun set(t: T, u: S)
}

private object DefaultBooleanConverter :
    ConfigValueConverter<Config.BooleanEntry, Boolean?, Boolean> {

  override fun getDefault(t: Config.BooleanEntry): Boolean = t.defaultValue
  override fun getWithoutDefault(t: Config.BooleanEntry): Boolean = t.value
  override fun getWithDefault(t: Config.BooleanEntry): Boolean = t.value
  override fun set(t: Config.BooleanEntry, u: Boolean?) = t.set(u)
}

object DefaultStringConverter : ConfigValueConverter<Config.StringEntry, String?, String?> {
  override fun getDefault(t: Config.StringEntry): String? = t.defaultValue
  override fun getWithoutDefault(t: Config.StringEntry): String? = t.valueWithoutDefault
  override fun getWithDefault(t: Config.StringEntry): String? = t.value
  override fun set(t: Config.StringEntry, u: String?) = t.set(u)
}

sealed class UiNode<in T : Config.Entry, S, G>(val converter: ConfigValueConverter<T, S, G>)
class TextBox : UiNode<Config.StringEntry, String?, String?>(DefaultStringConverter)
class PasswordBox : UiNode<Config.StringEntry, String?, String?>(DefaultStringConverter)
class CheckBox : UiNode<Config.BooleanEntry, Boolean?, Boolean>(DefaultBooleanConverter)
class ActionButton(val text: String, val action: () -> Boolean) :
    UiNode<Config.StringEntry, String?, String?>(DefaultStringConverter)

class NumberBox @JvmOverloads constructor(val min: Int = 0, val max: Int = 100) :
    UiNode<Config.StringEntry, Int, Int>(
        object : ConfigValueConverter<Config.StringEntry, Int, Int> {
          override fun getDefault(t: Config.StringEntry): Int = try {
            t.defaultValue?.toInt() ?: min
          } catch (e: NumberFormatException) {
            min
          }

          override fun getWithoutDefault(t: Config.StringEntry): Int = getWithDefault(t)

          override fun getWithDefault(t: Config.StringEntry): Int = try {
            t.valueWithoutDefault?.toInt() ?: getDefault(t)
          } catch (e: NumberFormatException) {
            getDefault(t)
          }

          override fun set(t: Config.StringEntry, u: Int) = t.set(u.toString())
        }
    )

/**
 * A choice for dropdown boxes.
 */
interface Choice<out I : Any> {

  val id: I
  val displayName: String
}

class StringChoice(override val id: String, override val displayName: String) : Choice<String>

/**
 * A dropdown box.
 *
 * @param refresh a function to call for choice items. If the function returns null, the list is not updated.
 * @param converter a ConfigValueConverter (most of the time it's DefaultStringConverter)
 * @param lazy should be set to true if refresh is slow
 *
 * @param I the type of ID for choices (most of the time it's String)
 * @param T the type of choice (most of the time it's StringChoice)
 */
class ChoiceBox<I : Any, out T : Choice<I>> @JvmOverloads constructor(val refresh: () -> List<T>?,
    converter: ConfigValueConverter<Config.StringEntry, I?, I?>, val lazy: Boolean = false) :
    UiNode<Config.StringEntry, I?, I?>(converter)

/**
 * A button which lets the user choose a file or directory.
 *
 * The current value is shown in an uneditable TextBox next to the button.
 *
 * @param isFolder whether to choose a folder (default true)
 */
class FileChooserButton @JvmOverloads constructor(val isFolder: Boolean = true) :
    UiNode<Config.StringEntry, String?, String?>(DefaultStringConverter)
