package com.github.bjoernpetersen.jmusicbot;

/**
 * An exception thrown by Plugin initialization methods.
 */
public final class InitializationException extends Exception {

  public InitializationException() {
    super();
  }

  public InitializationException(String message) {
    super(message);
  }

  public InitializationException(String message, Throwable cause) {
    super(message, cause);
  }

  public InitializationException(Throwable cause) {
    super(cause);
  }
}
