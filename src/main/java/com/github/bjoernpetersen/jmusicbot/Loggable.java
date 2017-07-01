package com.github.bjoernpetersen.jmusicbot;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public interface Loggable {

  default Logger createLogger() {
    return Logger.getLogger(getClass().getName());
  }

  default Logger getLogger() {
    return createLogger();
  }

  default void logSevere(String msg) {
    getLogger().severe(msg);
  }

  default void logSevere(String msg, Throwable throwable) {
    getLogger().log(Level.SEVERE, msg, throwable);
  }

  default void logWarning(String msg) {
    getLogger().warning(msg);
  }

  default void logWarning(String msg, Throwable throwable) {
    getLogger().log(Level.WARNING, msg, throwable);
  }

  default void logInfo(String msg) {
    getLogger().info(msg);
  }

  default void logConfig(String msg) {
    getLogger().config(msg);
  }

  default void logFine(String msg) {
    getLogger().fine(msg);
  }

  default void logFiner(String msg) {
    getLogger().finer(msg);
  }

  default void logFinest(String msg) {
    getLogger().finest(msg);
  }

  default void logSevere(String msg, Object... formatArgs) {
    getLogger().severe(String.format(msg, formatArgs));
  }

  default void logSevere(Throwable throwable, String msg, Object... formatArgs) {
    getLogger().log(Level.SEVERE, String.format(msg, formatArgs), throwable);
  }

  default void logWarning(String msg, Object... formatArgs) {
    getLogger().warning(String.format(msg, formatArgs));
  }

  default void logWarning(Throwable throwable, String msg, Object... formatArgs) {
    getLogger().log(Level.WARNING, String.format(msg, formatArgs), throwable);
  }

  default void logInfo(String msg, Object... formatArgs) {
    getLogger().info(String.format(msg, formatArgs));
  }

  default void logConfig(String msg, Object... formatArgs) {
    getLogger().config(String.format(msg, formatArgs));
  }

  default void logFine(String msg, Object... formatArgs) {
    getLogger().fine(String.format(msg, formatArgs));
  }

  default void logFiner(String msg, Object... formatArgs) {
    getLogger().finer(String.format(msg, formatArgs));
  }

  default void logFinest(String msg, Object... formatArgs) {
    getLogger().finest(String.format(msg, formatArgs));
  }

  default void logSevere(Supplier<String> msgSupplier) {
    getLogger().severe(msgSupplier);
  }

  default void logWarning(Supplier<String> msgSupplier) {
    getLogger().warning(msgSupplier);
  }

  default void logInfo(Supplier<String> msgSupplier) {
    getLogger().info(msgSupplier);
  }

  default void logConfig(Supplier<String> msgSupplier) {
    getLogger().config(msgSupplier);
  }

  default void logFine(Supplier<String> msgSupplier) {
    getLogger().fine(msgSupplier);
  }

  default void logFiner(Supplier<String> msgSupplier) {
    getLogger().finer(msgSupplier);
  }

  default void logFinest(Supplier<String> msgSupplier) {
    getLogger().finest(msgSupplier);
  }
}
