package com.github.bjoernpetersen.jmusicbot.platform

import android.content.Context


object ContextHolder {
  private var _context: Context? = null

  val context: Context
    get() = _context ?: throw IllegalStateException()

  fun initialize(contextSupplier: ContextSupplier) {
    _context = contextSupplier.supply()
  }
}