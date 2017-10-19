package com.github.bjoernpetersen.jmusicbot.provider

/**
 * <p>Indicates that a Suggester cannot fulfill a suggestion request right now.</p>
 * <p>This Exception does not imply that the suggester will be broken in the future.</p>
 */
class BrokenSuggesterException : Exception {

  constructor() : super()
  constructor(message: String?) : super(message)
  constructor(message: String?, cause: Throwable?) : super(message, cause)
  constructor(cause: Throwable?) : super(cause)
}