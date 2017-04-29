package com.github.bjoernpetersen.jmusicbot.api;

final class LookupException extends Exception {

  private final int status;

  private LookupException(String missingParam) {
    super("Missing query parameter: " + missingParam);
    this.status = 400;
  }

  private LookupException(String providerName, String songId) {
    super(String.format("No such song '%s' with provider '%s'", songId, providerName));
    this.status = 404;
  }

  int getStatus() {
    return status;
  }

  public static LookupException notFound(String providerName, String songId) {
    return new LookupException(providerName, songId);
  }

  public static LookupException badRequest(String missingParam) {
    return new LookupException(missingParam);
  }
}
