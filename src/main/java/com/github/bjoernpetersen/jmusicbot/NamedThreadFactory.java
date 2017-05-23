package com.github.bjoernpetersen.jmusicbot;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

public final class NamedThreadFactory implements ThreadFactory {

  @Nonnull
  private final ThreadGroup group;
  @Nonnull
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  @Nonnull
  private final Supplier<String> prefixSupplier;

  public NamedThreadFactory(@Nonnull String name) {
    this(createSupplier(name));
  }

  @Nonnull
  private static Supplier<String> createSupplier(@Nonnull String name) {
    Objects.requireNonNull(name);
    return () -> name;
  }

  public NamedThreadFactory(@Nonnull Supplier<String> nameSupplier) {
    SecurityManager s = System.getSecurityManager();
    this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    this.prefixSupplier = Objects.requireNonNull(nameSupplier);
  }

  @Nonnull
  private String getPrefix() {
    return prefixSupplier.get() + "-";
  }

  @Override
  public Thread newThread(@Nonnull Runnable runnable) {
    Thread thread = new Thread(
        group,
        runnable,
        getPrefix() + threadNumber.getAndIncrement(),
        0
    );
    if (thread.isDaemon()) {
      thread.setDaemon(false);
    }
    if (thread.getPriority() != Thread.NORM_PRIORITY) {
      thread.setPriority(Thread.NORM_PRIORITY);
    }
    return thread;
  }
}
