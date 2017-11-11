package com.github.bjoernpetersen.jmusicbot.user;

import com.google.common.base.Splitter;
import com.google.common.collect.Streams;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

final class Database implements Closeable {

  @Nonnull
  private final Connection connection;

  @Nonnull
  private final PreparedStatement createUser;
  @Nonnull
  private final PreparedStatement getUser;
  @Nonnull
  private final PreparedStatement updatePassword;
  @Nonnull
  private final PreparedStatement updatePermissions;
  @Nonnull
  private final PreparedStatement dropUser;
  @Nonnull
  private final PreparedStatement getUsers;

  Database(@Nonnull String databaseUrl) throws SQLException {
    this.connection = DriverManager.getConnection(databaseUrl);

    try (Statement statement = connection.createStatement()) {
      statement.execute("CREATE TABLE IF NOT EXISTS users("
          + "id TEXT PRIMARY KEY NOT NULL,"
          + "name TEXT NOT NULL,"
          + "password TEXT NOT NULL,"
          + "permissions TEXT NOT NULL)");
    }

    this.createUser = connection.prepareStatement(
        "INSERT OR ABORT INTO users(id, name, password, permissions) VALUES(?, ?, ?, ?)"
    );
    this.getUser = connection
        .prepareStatement("SELECT name, password, permissions FROM users WHERE id=?");
    this.updatePassword = connection.prepareStatement("UPDATE users SET password=? WHERE id=?");
    this.updatePermissions = connection
        .prepareStatement("UPDATE users SET permissions=? WHERE id=?");
    this.dropUser = connection.prepareStatement("DELETE FROM users WHERE id=?");
    this.getUsers = connection.prepareStatement("SELECT * FROM users");
  }

  @Nonnull
  User createUser(String user, String passwordHash) throws DuplicateUserException {
    synchronized (createUser) {
      try {
        createUser.clearParameters();
        createUser.setString(1, user.toLowerCase());
        createUser.setString(2, user);
        createUser.setString(3, passwordHash);
        createUser.setString(4, "");
        createUser.execute();
      } catch (SQLException e) {
        throw new DuplicateUserException(e);
      }
    }
    return new User(user, passwordHash, Collections.emptySet());
  }

  @Nonnull
  User getUser(String user) throws UserNotFoundException {
    synchronized (getUser) {
      try {
        getUser.clearParameters();
        getUser.setString(1, user.toLowerCase());
        String name;
        String hash;
        String permissionString;
        try (ResultSet resultSet = getUser.executeQuery()) {
          if (!resultSet.next()) {
            throw new UserNotFoundException("No such user: " + user);
          }
          name = resultSet.getString("name");
          hash = resultSet.getString("password");
          permissionString = resultSet.getString("permissions");
        }
        Set<Permission> permissions = getPermissions(permissionString).collect(Collectors.toSet());
        return new User(name, hash, permissions);
      } catch (SQLException e) {
        throw new UserNotFoundException(e);
      }
    }
  }

  @Nonnull
  private Stream<Permission> getPermissions(String permissionString) {
    return Streams.stream(Splitter.on(',')
        .omitEmptyStrings()
        .split(permissionString))
        .map(Permission::matchByLabel);
  }

  @Nonnull
  User updatePassword(User user, String passwordHash) throws SQLException {
    synchronized (updatePassword) {
      updatePassword.clearParameters();
      updatePassword.setString(1, passwordHash);
      updatePassword.setString(2, user.getName().toLowerCase());
      updatePassword.execute();
    }
    user.invalidate();
    return new User(user.getName(), passwordHash, user.getPermissions());
  }

  @Nonnull
  User updatePermissions(User user, Set<Permission> newPermissions) throws SQLException {
    synchronized (updatePermissions) {
      updatePermissions.clearParameters();
      String permissions = newPermissions.stream()
          .map(Permission::getLabel)
          .reduce("", (l, r) -> l + ',' + r);
      updatePermissions.setString(1, permissions);
      updatePermissions.setString(2, user.getName().toLowerCase());
      updatePermissions.execute();
    }
    User newUser = new User(user.getName(), user.getHash(), newPermissions);
    user.invalidate();
    return newUser;
  }

  void dropUser(User user) throws SQLException {
    synchronized (dropUser) {
      dropUser.clearParameters();
      dropUser.setString(1, user.getName().toLowerCase());
      dropUser.execute();
    }
    user.invalidate();
  }

  List<User> getUsers() throws SQLException {
    synchronized (getUsers) {
      try (ResultSet resultSet = getUsers.executeQuery()) {
        List<User> users = new LinkedList<>();
        while (resultSet.next()) {
          String name = resultSet.getString("name");
          String hash = resultSet.getString("password");
          String permissionString = resultSet.getString("permissions");
          Set<Permission> permissions = getPermissions(permissionString)
              .collect(Collectors.toSet());
          users.add(new User(name, hash, permissions));
        }

        return Collections.unmodifiableList(users);
      }
    }
  }

  @Override
  public void close() throws IOException {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
}
