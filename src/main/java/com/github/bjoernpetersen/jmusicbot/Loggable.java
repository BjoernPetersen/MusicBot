package com.github.bjoernpetersen.jmusicbot;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

public interface Loggable {

  @Nonnull
  default Logger createLogger() {
    return Logger.getLogger(getClass().getName());
  }

  @Nonnull
  default Logger getLogger() {
    return createLogger();
  }

  default void logSevere(@Nonnull String msg) {
    log(Level.SEVERE, msg);
  }

  default void logSevere(@Nonnull Throwable throwable, @Nonnull String msg) {
    log(Level.SEVERE, throwable, msg);
  }

  default void logWarning(@Nonnull String msg) {
    log(Level.WARNING, msg);
  }

  default void logWarning(@Nonnull Throwable throwable, @Nonnull String msg) {
    log(Level.WARNING, throwable, msg);
  }

  default void logInfo(@Nonnull String msg) {
    log(Level.INFO, msg);
  }

  default void logInfo(@Nonnull Throwable throwable, @Nonnull String msg) {
    log(Level.INFO, throwable, msg);
  }

  default void logConfig(@Nonnull String msg) {
    log(Level.CONFIG, msg);
  }

  default void logFine(@Nonnull String msg) {
    log(Level.FINE, msg);
  }

  default void logFine(@Nonnull Throwable throwable, @Nonnull String msg) {
    log(Level.FINE, throwable, msg);
  }

  default void logFiner(@Nonnull String msg) {
    log(Level.FINER, msg);
  }

  default void logFinest(@Nonnull String msg) {
    log(Level.FINEST, msg);
  }

  default void logSevere(@Nonnull String msg, @Nonnull Object... formatArgs) {
    log(Level.SEVERE, msg, formatArgs);
  }

  default void logSevere(@Nonnull Throwable throwable, @Nonnull String msg,
      @Nonnull Object... formatArgs) {
    log(Level.SEVERE, throwable, msg, formatArgs);
  }

  default void logWarning(@Nonnull String msg, @Nonnull Object... formatArgs) {
    log(Level.WARNING, msg, formatArgs);
  }

  default void logWarning(@Nonnull Throwable throwable, @Nonnull String msg,
      @Nonnull Object... formatArgs) {
    log(Level.WARNING, throwable, msg, formatArgs);
  }

  default void logInfo(@Nonnull String msg, @Nonnull Object... formatArgs) {
    log(Level.INFO, msg, formatArgs);
  }

  default void logInfo(@Nonnull Throwable throwable, @Nonnull String msg,
      @Nonnull Object... formatArgs) {
    log(Level.INFO, throwable, msg, formatArgs);
  }

  default void logConfig(@Nonnull String msg, @Nonnull Object... formatArgs) {
    log(Level.CONFIG, msg, formatArgs);
  }

  default void logFine(@Nonnull String msg, @Nonnull Object... formatArgs) {
    log(Level.FINE, msg, formatArgs);
  }

  default void logFine(@Nonnull Throwable throwable, @Nonnull String msg,
      @Nonnull Object... formatArgs) {
    log(Level.FINE, throwable, msg, formatArgs);
  }

  default void logFiner(@Nonnull String msg, @Nonnull Object... formatArgs) {
    log(Level.FINER, msg, formatArgs);
  }

  default void logFinest(@Nonnull String msg, @Nonnull Object... formatArgs) {
    log(Level.FINEST, msg, formatArgs);
  }

  default void logSevere(@Nonnull Supplier<String> msgSupplier) {
    log(Level.SEVERE, msgSupplier);
  }

  default void logWarning(@Nonnull Supplier<String> msgSupplier) {
    log(Level.WARNING, msgSupplier);
  }

  default void logInfo(@Nonnull Supplier<String> msgSupplier) {
    log(Level.INFO, msgSupplier);
  }

  default void logConfig(@Nonnull Supplier<String> msgSupplier) {
    log(Level.CONFIG, msgSupplier);
  }

  default void logFine(@Nonnull Supplier<String> msgSupplier) {
    log(Level.FINE, msgSupplier);
  }

  default void logFiner(@Nonnull Supplier<String> msgSupplier) {
    log(Level.FINER, msgSupplier);
  }

  default void logFinest(@Nonnull Supplier<String> msgSupplier) {
    log(Level.FINEST, msgSupplier);
  }

  default void log(@Nonnull Level level, @Nonnull Supplier<String> msgSupplier) {
    StackTraceElement caller = findCaller();
    getLogger().logp(level, caller.getClassName(), caller.getMethodName(), msgSupplier);
  }

  default void log(@Nonnull Level level, @Nonnull String msg, @Nonnull Object... formatArgs) {
    StackTraceElement caller = findCaller();
    getLogger().logp(
        level,
        caller.getClassName(),
        caller.getMethodName(),
        String.format(msg, formatArgs)
    );
  }

  default void log(@Nonnull Level level, @Nonnull Throwable throwable, @Nonnull String msg,
      @Nonnull Object... formatArgs) {
    StackTraceElement caller = findCaller();
    getLogger().logp(
        level,
        caller.getClassName(),
        caller.getMethodName(),
        String.format(msg, formatArgs),
        throwable
    );
  }

  static StackTraceElement findCaller() {
    String className = Loggable.class.getName();
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    Spliterator<StackTraceElement> iterator = Arrays.spliterator(stackTrace, 1, stackTrace.length);
    StackTraceElement[] elementRef = new StackTraceElement[1];
    while (iterator.tryAdvance(element -> elementRef[0] = element)) {
      StackTraceElement element = elementRef[0];
      if (!element.getClassName().equals(className)) {
        return element;
      }
    }
    throw new IllegalStateException();
  }
}
