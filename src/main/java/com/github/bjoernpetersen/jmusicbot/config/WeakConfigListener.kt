package com.github.bjoernpetersen.jmusicbot.config

import java.lang.ref.WeakReference

class WeakConfigListener<T>(listener: ConfigListener<T>) : ConfigListener<T> {

  private val listener: WeakReference<ConfigListener<T>> = WeakReference(listener)

  override fun onChange(oldValue: T, newValue: T) {
    val listener = this.listener.get()
    listener?.onChange(oldValue, newValue)
  }
}
