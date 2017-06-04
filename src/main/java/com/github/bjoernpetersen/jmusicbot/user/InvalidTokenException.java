package com.github.bjoernpetersen.jmusicbot.user;

public final class InvalidTokenException extends Exception {

  public InvalidTokenException() {
  }

  public InvalidTokenException(String message) {
    super(message);
  }

  public InvalidTokenException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidTokenException(Throwable cause) {
    super(cause);
  }
}
