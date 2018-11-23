package net.bjoernpetersen.musicbot.api.config

import com.google.common.annotations.Beta
import java.io.File

sealed class UiNode<T>
object TextBox : UiNode<String>()
object PasswordBox : UiNode<String>()
object CheckBox : UiNode<Boolean>()
// TODO sure about that?
@Beta
data class ActionButton<T>(
    val label: String,
    val descriptor: (T) -> String,
    val action: () -> Boolean) : UiNode<T>()

data class NumberBox @JvmOverloads constructor(val min: Int = 0, val max: Int = 100) : UiNode<Int>()
data class FileChooser(val isDirectory: Boolean = true) : UiNode<File>()
@Beta
data class ChoiceBox<T> @JvmOverloads constructor(
    val descriptor: (T) -> String,
    val refresh: () -> List<T>?,
    val lazy: Boolean = false) : UiNode<T>()
