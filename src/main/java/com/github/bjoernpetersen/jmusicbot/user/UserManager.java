package com.github.bjoernpetersen.jmusicbot.user;

import com.github.bjoernpetersen.jmusicbot.config.Config;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacSigner;
import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.Period;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.mindrot.jbcrypt.BCrypt;

public final class UserManager implements Closeable {

  @Nonnull
  private final Config.StringEntry signatureKey;
  @Nonnull
  private final String guestSignatureKey;
  @Nonnull
  private final Database database;

  @Nonnull
  private final Map<String, User> temporaryUsers;
  @Nonnull
  private final LoadingCache<String, User> users;

  public UserManager(@Nonnull Config config, @Nonnull String databaseUrl) throws SQLException {
    this.signatureKey = config.secret(getClass(), "signatureKey", "");
    this.guestSignatureKey = createSignatureKey();
    this.database = new Database(databaseUrl);

    this.temporaryUsers = new HashMap<>(32);
    this.users = CacheBuilder.newBuilder()
        .maximumSize(64)
        .build(new CacheLoader<String, User>() {
          @Override
          public User load(@Nonnull String name) throws Exception {
            User user = temporaryUsers.get(name);
            return user == null ? database.getUser(name) : user;
          }
        });
  }

  @Nonnull
  public User createTemporaryUser(String name, String uuid) throws DuplicateUserException {
    try {
      // TODO create "hasUser" method in Database
      getUser(name);
      throw new DuplicateUserException("User already exists: " + name);
    } catch (UserNotFoundException expected) {
      User user = new User(name, uuid);
      temporaryUsers.put(name, user);
      return user;
    }
  }

  @Nonnull
  public User getUser(String name) throws UserNotFoundException {
    User user;
    try {
      user = users.get(name);
    } catch (ExecutionException e) {
      throw (UserNotFoundException) e.getCause();
    }

    if (user.isInvalid()) {
      users.invalidate(user);
      return getUser(name);
    } else {
      return user;
    }
  }

  /**
   * <p>Updates the specified users password.</p>
   *
   * <p>If the user is a guest, this will make them a full user.</p>
   *
   * @param user the user name
   * @param password the new password
   * @return a new, valid user object
   * @throws DuplicateUserException if the specified user was a guest and a full user with the same
   * name already existed
   * @throws SQLException if any SQL errors occur
   */
  @Nonnull
  public User updateUser(User user, String password) throws DuplicateUserException, SQLException {
    if (user.isInvalid() || password.isEmpty()) {
      throw new IllegalArgumentException();
    }

    String name = user.getName();
    User newUser;
    if (user.isTemporary()) {
      newUser = database.createUser(user.getName(), hash(password));
      temporaryUsers.remove(name);
    } else {
      newUser = database.updatePassword(user, hash(password));
    }

    users.put(name, newUser);
    return newUser;
  }

  @Nonnull
  public User updateUser(User user, Set<Permission> permissions) throws SQLException {
    if (user.isInvalid() || user.isTemporary()) {
      throw new IllegalArgumentException();
    }

    User newUser = database.updatePermissions(user, permissions);
    users.put(newUser.getName(), newUser);
    return newUser;
  }

  public void deleteUser(User user) throws SQLException {
    String name = user.getName();
    database.dropUser(user);
    users.invalidate(name);
  }

  /**
   * Gets all full users.
   *
   * @return a list of users
   * @throws SQLException if any SQL errors occur
   */
  public List<User> getUsers() throws SQLException {
    return database.getUsers();
  }

  @Nonnull
  public String toToken(User user) {
    if (user.isInvalid()) {
      throw new IllegalArgumentException();
    }

    String signatureKey;
    if (user.isTemporary()) {
      signatureKey = this.guestSignatureKey;
    } else {
      signatureKey = getSignatureKey();
    }
    JwtBuilder builder = Jwts.builder()
        .setSubject(user.getName())
        .setIssuedAt(new Date())
        .signWith(SignatureAlgorithm.HS512, signatureKey)
        .setExpiration(Date.from(Instant.now().plus(Period.ofDays(7))));

    for (Permission permission : user.getPermissions()) {
      builder.claim(permission.getLabel(), true);
    }

    return builder.compact();
  }

  @Nonnull
  public User fromToken(String token) throws InvalidTokenException {
    Jws<Claims> parsed;
    try {
      parsed = Jwts.parser()
          .setSigningKey(getSignatureKey())
          .parseClaimsJws(token);
    } catch (JwtException e) {
      // try again with guest key
      try {
        parsed = Jwts.parser()
            .setSigningKey(guestSignatureKey)
            .parseClaimsJws(token);
      } catch (JwtException e1) {
        e1.addSuppressed(e);
        throw new InvalidTokenException(e1);
      }
    }

    String name = parsed.getBody().getSubject();
    if (name == null) {
      throw new InvalidTokenException("Name missing");
    }

    try {
      return getUser(name);
    } catch (UserNotFoundException e) {
      throw new InvalidTokenException(e);
    }
  }

  @Nonnull
  private String getSignatureKey() {
    Optional<String> configKey = signatureKey.get();
    if (configKey.isPresent()) {
      return configKey.get();
    } else {
      String key = createSignatureKey();
      signatureKey.set(key);
      return key;
    }
  }

  @Nonnull
  private String createSignatureKey() {
    byte[] encoded = Base64.getEncoder().encode(MacSigner.generateKey().getEncoded());
    return new String(encoded);
  }

  @Nonnull
  private String hash(String password) {
    return BCrypt.hashpw(password, BCrypt.gensalt());
  }

  @Override
  public void close() throws IOException {
    database.close();
  }
}
