package com.github.bjoernpetersen.jmusicbot.user;

public final class DuplicateUserException extends Exception {

  public DuplicateUserException() {
  }

  public DuplicateUserException(String message) {
    super(message);
  }

  public DuplicateUserException(String message, Throwable cause) {
    super(message, cause);
  }

  public DuplicateUserException(Throwable cause) {
    super(cause);
  }
}
