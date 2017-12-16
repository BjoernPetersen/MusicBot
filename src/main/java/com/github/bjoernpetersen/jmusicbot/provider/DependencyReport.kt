package com.github.bjoernpetersen.jmusicbot.provider

import com.github.bjoernpetersen.jmusicbot.Plugin

/**
 * Used to describe dependencies of a plugin.
 *
 * A report may contain [required] and [optional] dependencies.
 *
 * @param P the type of dependency
 */
interface DependencyReport<P : Plugin> {

  /**
   * Registers a required dependency. The reporter will not be initialized without this dependency.
   * @param dependency a dependency class
   */
  fun required(dependency: Class<out P>)

  /**
   * Registers an optional dependency. The reporter will get an instance of this dependency if there is one available.
   * @param dependency a dependency class
   */
  fun optional(dependency: Class<out P>)
}

class DependencyReportImpl<P : Plugin> : DependencyReport<P> {
  // if plugin keeps the report instance around, it might attempt to register dependencies later.
  private var freeze = false
  private val required: MutableSet<Class<out P>> = mutableSetOf()
  private val optional: MutableSet<Class<out P>> = mutableSetOf()

  override fun required(dependency: Class<out P>) {
    if (!freeze) required.add(dependency) else throw IllegalStateException()
  }

  override fun optional(dependency: Class<out P>) {
    if (!freeze) optional.add(dependency) else throw IllegalStateException()
  }

  private fun freeze() {
    freeze = true
  }

  fun getResult(): Result<P> {
    freeze()
    return Result(required, optional)
  }

  data class Result<out P : Plugin>(val required: Set<Class<out P>>, val optional: Set<Class<out P>>) {
    val size = required.size + optional.size
  }
}
