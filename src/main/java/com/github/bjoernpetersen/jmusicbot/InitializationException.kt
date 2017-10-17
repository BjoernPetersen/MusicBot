package com.github.bjoernpetersen.jmusicbot

import java.lang.Exception


class InitializationException : Exception {
  constructor() : super()
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
  constructor(cause: Throwable) : super(cause)
  constructor(message: String, cause: Throwable, enableSuppression: Boolean,
      writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace)
}

class CancelException : Exception {
  constructor() : super()
  constructor(message: String) : super(message)
  constructor(message: String, cause: Throwable) : super(message, cause)
  constructor(cause: Throwable) : super(cause)
}
