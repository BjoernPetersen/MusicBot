package com.github.bjoernpetersen.jmusicbot.config.ui

import com.github.bjoernpetersen.jmusicbot.config.Config
import com.github.bjoernpetersen.jmusicbot.config.ConfigChecker
import com.github.bjoernpetersen.jmusicbot.config.ConfigStorageAdapter
import java.util.*
import kotlin.collections.ArrayList


sealed class UiNode {
  abstract val children: Collection<UiNode>
}

sealed class Container : UiNode() {
  enum class Orientation {
    HORIZONTAL, VERTICAL
  }

  override val children: MutableList<UiNode> = ArrayList()
}

class LinearLayout(val orientation: Orientation = Orientation.HORIZONTAL) : Container()

class TextBox() : UiNode() {
  override val children: Set<UiNode>
    get() = emptySet()
}

class PasswordBox() : UiNode() {
  override val children: Collection<UiNode>
    get() = emptySet()
}

class NumberBox() : UiNode() {
  override val children: Collection<UiNode>
    get() = emptySet()
}

class CheckBox() : UiNode() {
  override val children: Set<UiNode>
    get() = emptySet()
}

class ChoiceBox(val refresh: () -> List<String>?) : UiNode() {
  override val children: Collection<UiNode>
    get() = emptySet()
}
// TODO: file chooser button

fun test() {
  val c = Config(object:ConfigStorageAdapter {
    override fun loadPlaintext(): MutableMap<String, String> {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadSecrets(): MutableMap<String, String> {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storePlaintext(plain: MutableMap<String, String>) {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun storeSecrets(secrets: MutableMap<String, String>) {
      TODO(
          "not implemented") //To change body of created functions use File | Settings | File Templates.
    }
  })
  c.StringEntry(ChoiceBox::class.java, "", "", true);
}
