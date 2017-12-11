package com.github.bjoernpetersen.jmusicbot.user

import com.github.bjoernpetersen.jmusicbot.provider.Suggester

/**
 * User permissions to perform certain actions.
 */
enum class Permission(
    /**
     * A human-readable label for this permission.
     */
    val label: String) {

  /**
   * The permission to skip the current song or remove songs from the queue.
   * Note that a user is always allowed to remove a song from the queue which he added himself.
   */
  SKIP("skip"),
  /**
   * The permission to remove songs from the upcoming suggestions of a suggester.
   *
   * Note that doing this will also trigger [Suggester.dislike], thus affecting future suggestions.
   */
  DISLIKE("dislike"),
  /**
   * The permission to move songs around in the queue.
   */
  MOVE("move");

  companion object {
    /**
     * Finds the permission with the specified label.
     * @throws IllegalArgumentException if there is no Permission with that label
     */
    @JvmStatic
    fun matchByLabel(label: String): Permission {
      return Permission.values()
          .asSequence()
          .filter { it.label == label }
          .firstOrNull() ?: throw IllegalArgumentException()
    }
  }
}
