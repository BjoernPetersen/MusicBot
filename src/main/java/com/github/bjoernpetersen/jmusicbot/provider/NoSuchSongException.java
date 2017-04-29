package com.github.bjoernpetersen.jmusicbot.provider;

public final class NoSuchSongException extends Exception {

  public NoSuchSongException() {
  }

  public NoSuchSongException(String message) {
    super(message);
  }

  public NoSuchSongException(String message, Throwable cause) {
    super(message, cause);
  }

  public NoSuchSongException(Throwable cause) {
    super(cause);
  }
}
