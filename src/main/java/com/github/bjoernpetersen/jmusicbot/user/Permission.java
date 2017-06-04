package com.github.bjoernpetersen.jmusicbot.user;

import javax.annotation.Nonnull;

public enum Permission {
  SKIP("skip");

  private final String name;

  Permission(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static Permission matchByName(@Nonnull String name) {
    for (Permission permission : values()) {
      if (permission.getName().equals(name)) {
        return permission;
      }
    }
    throw new IllegalArgumentException("Unknown permission: " + name);
  }
}
