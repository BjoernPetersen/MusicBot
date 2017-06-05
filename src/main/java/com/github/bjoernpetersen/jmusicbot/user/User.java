package com.github.bjoernpetersen.jmusicbot.user;

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.mindrot.jbcrypt.BCrypt;

/**
 * <p>Represents a user.</p>
 *
 * There are two types of users: <ol> <li>Guest/temporary users. Can't have permissions and is
 * dropped when the bot stops.</li> <li>Full users. Can have permissions and are permanent.</li>
 * </ol>
 *
 * <p> A user object can become invalid if the users permissions change or the user is deleted. In
 * that case, {@link #isInvalid()} will return true and other methods will throw an
 * IllegalStateException. </p>
 */
public final class User {

  private boolean invalid;
  @Nonnull
  private final String name;
  @Nullable
  private final String hash;
  @Nullable
  private final String uuid;
  @Nonnull
  private final Set<Permission> permissions;

  /**
   * Creates a full user with a password hash and permissions.
   *
   * @param name the users name
   * @param hash his password hash
   * @param permissions his permissions
   */
  User(@Nonnull String name, @Nonnull String hash, @Nonnull Set<Permission> permissions) {
    this.invalid = false;
    this.name = name;
    this.hash = hash;
    this.uuid = null;
    this.permissions = Collections.unmodifiableSet(permissions);
  }

  /**
   * Creates a guest user authenticated by the given uuid. Can't have permissions.
   *
   * @param name the users name
   * @param uuid a uuid
   */
  User(@Nonnull String name, @Nullable String uuid) {
    this.invalid = false;
    this.name = name;
    this.hash = null;
    this.uuid = uuid;
    this.permissions = Collections.emptySet();
  }

  @Nonnull
  public String getName() {
    return name;
  }

  @Nonnull
  String getHash() {
    if (invalid || hash == null) {
      throw new IllegalStateException();
    }
    return hash;
  }

  public boolean hasPassword(@Nonnull String password) {
    if (invalid) {
      throw new IllegalStateException();
    }
    return BCrypt.checkpw(password, hash);
  }

  String getUuid() {
    if (invalid || uuid == null) {
      throw new IllegalStateException();
    }
    return uuid;
  }

  public boolean hasUuid(String uuid) {
    if (invalid || this.uuid == null) {
      throw new IllegalStateException();
    }
    return this.uuid.equals(uuid);
  }

  @Nonnull
  public Set<Permission> getPermissions() {
    if (invalid) {
      throw new IllegalStateException();
    }
    return permissions;
  }

  /**
   * Determines whether this is a guest user.
   *
   * @return whether this is a guest
   */
  public boolean isTemporary() {
    return hash == null;
  }

  /**
   * Gets the validity state of this object.
   * If the object is invalid, calls to any other methods result in a IllegalStateException.
   *
   * @return whether this user is invalid
   */
  public boolean isInvalid() {
    return invalid;
  }

  void invalidate() {
    this.invalid = true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    User user = (User) o;

    if (invalid != user.invalid) {
      return false;
    }
    return name.equals(user.name);
  }

  @Override
  public int hashCode() {
    int result = (invalid ? 1 : 0);
    result = 31 * result + name.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "User{" +
        "invalid=" + invalid +
        ", name='" + name + '\'' +
        ", hash='" + hash + '\'' +
        ", uuid='" + uuid + '\'' +
        ", permissions=" + permissions +
        '}';
  }
}
