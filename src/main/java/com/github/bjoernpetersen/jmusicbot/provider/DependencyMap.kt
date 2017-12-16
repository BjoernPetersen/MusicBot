package com.github.bjoernpetersen.jmusicbot.provider

import com.github.bjoernpetersen.jmusicbot.Plugin

interface DependencyMap<P : Plugin> {
  operator fun <Base : P> get(baseClass: Class<Base>): Base?
}

class DependencyMapWrapper<P : Plugin>(private val wrapped: Map<Class<out P>, P>) : DependencyMap<P> {
  override operator fun <Base : P> get(baseClass: Class<Base>): Base? {
    return baseClass.cast(wrapped[baseClass])
  }
}

class DependencyMapLookup<P : Plugin>(private val lookup: (Class<out P>) -> P?) : DependencyMap<P> {
  override operator fun <Base : P> get(baseClass: Class<Base>): Base? {
    return baseClass.cast(lookup(baseClass))
  }
}
