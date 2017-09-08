package com.github.bjoernpetersen.jmusicbot.user


enum class Permission(val label: String) {
  SKIP("skip"), DISLIKE("dislike");

  companion object {
    @JvmStatic
    fun matchByLabel(label: String): Permission {
      Permission.values()
          .filter { it.label == label }
          .forEach { return it }
      throw IllegalArgumentException()
    }
  }
}
