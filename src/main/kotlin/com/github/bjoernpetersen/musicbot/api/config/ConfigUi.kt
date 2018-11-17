package com.github.bjoernpetersen.musicbot.api.config

import java.io.File

sealed class UiNode<T>
object TextBox : UiNode<String>()
object PasswordBox : UiNode<String>()
object CheckBox : UiNode<Boolean>()
// TODO sure about that?
data class ActionButton(val label: String, val action: () -> Boolean) : UiNode<String>()

data class NumberBox @JvmOverloads constructor(val min: Int = 0, val max: Int = 100) : UiNode<Int>()
data class FileChooser(val isDirectory: Boolean = true) : UiNode<File>()
data class ChoiceBox<T> @JvmOverloads constructor(
    val refresh: () -> List<T>?,
    val lazy: Boolean = false) : UiNode<T>()
