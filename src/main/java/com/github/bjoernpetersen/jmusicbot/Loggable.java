package com.github.bjoernpetersen.jmusicbot;

import java.util.Arrays;
import java.util.Spliterator;
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
    log(Level.SEVERE, msg);
  }

  default void logSevere(String msg, Throwable throwable) {
    log(Level.SEVERE, msg, throwable);
  }

  default void logWarning(String msg) {
    log(Level.WARNING, msg);
  }

  default void logWarning(String msg, Throwable throwable) {
    log(Level.WARNING, msg, throwable);
  }

  default void logInfo(String msg) {
    log(Level.INFO, msg);
  }

  default void logConfig(String msg) {
    log(Level.CONFIG, msg);
  }

  default void logFine(String msg) {
    log(Level.FINE, msg);
  }

  default void logFiner(String msg) {
    log(Level.FINER, msg);
  }

  default void logFinest(String msg) {
    log(Level.FINEST, msg);
  }

  default void logSevere(String msg, Object... formatArgs) {
    log(Level.SEVERE, msg, formatArgs);
  }

  default void logSevere(Throwable throwable, String msg, Object... formatArgs) {
    log(Level.SEVERE, throwable, msg, formatArgs);
  }

  default void logWarning(String msg, Object... formatArgs) {
    log(Level.WARNING, msg, formatArgs);
  }

  default void logWarning(Throwable throwable, String msg, Object... formatArgs) {
    log(Level.WARNING, throwable, msg, formatArgs);
  }

  default void logInfo(String msg, Object... formatArgs) {
    log(Level.INFO, msg, formatArgs);
  }

  default void logConfig(String msg, Object... formatArgs) {
    log(Level.CONFIG, msg, formatArgs);
  }

  default void logFine(String msg, Object... formatArgs) {
    log(Level.FINE, msg, formatArgs);
  }

  default void logFiner(String msg, Object... formatArgs) {
    log(Level.FINER, msg, formatArgs);
  }

  default void logFinest(String msg, Object... formatArgs) {
    log(Level.FINEST, msg, formatArgs);
  }

  default void logSevere(Supplier<String> msgSupplier) {
    log(Level.SEVERE, msgSupplier);
  }

  default void logWarning(Supplier<String> msgSupplier) {
    log(Level.WARNING, msgSupplier);
  }

  default void logInfo(Supplier<String> msgSupplier) {
    log(Level.INFO, msgSupplier);
  }

  default void logConfig(Supplier<String> msgSupplier) {
    log(Level.CONFIG, msgSupplier);
  }

  default void logFine(Supplier<String> msgSupplier) {
    log(Level.FINE, msgSupplier);
  }

  default void logFiner(Supplier<String> msgSupplier) {
    log(Level.FINER, msgSupplier);
  }

  default void logFinest(Supplier<String> msgSupplier) {
    log(Level.FINEST, msgSupplier);
  }

  default void log(Level level, Supplier<String> msgSupplier) {
    StackTraceElement caller = findCaller();
    getLogger().logp(level, caller.getClassName(), caller.getMethodName(), msgSupplier);
  }

  default void log(Level level, String msg, Object... formatArgs) {
    StackTraceElement caller = findCaller();
    getLogger().logp(
        level,
        caller.getClassName(),
        caller.getMethodName(),
        String.format(msg, formatArgs)
    );
  }

  default void log(Level level, Throwable throwable, String msg, Object... formatArgs) {
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
