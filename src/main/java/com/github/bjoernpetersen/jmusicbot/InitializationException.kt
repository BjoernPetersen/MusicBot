package com.github.bjoernpetersen.jmusicbot

import java.lang.Exception

/**
 * An Exception occurring during the initialization of the MusicBot / a Plugin.
 */
class InitializationException : Exception {

  constructor() : super()
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
  constructor(cause: Throwable) : super(cause)
}

/**
 * An Exception thrown if the user cancels the MusicBot initialization.
 */
class CancelException : Exception {

  constructor() : super()
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
  constructor(cause: Throwable) : super(cause)
}
