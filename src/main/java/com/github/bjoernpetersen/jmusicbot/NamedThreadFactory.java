package com.github.bjoernpetersen.jmusicbot;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

public final class NamedThreadFactory implements ThreadFactory {

  @Nonnull
  private final ThreadGroup group;
  @Nonnull
  private final AtomicInteger threadNumber = new AtomicInteger(1);
  @Nonnull
  private final String prefix;

  public NamedThreadFactory(@Nonnull String name) {
    SecurityManager s = System.getSecurityManager();
    this.group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
    this.prefix = Objects.requireNonNull(name) + "-";
  }

  @Override
  public Thread newThread(@Nonnull Runnable runnable) {
    Thread thread = new Thread(
        group,
        runnable,
        prefix + threadNumber.getAndIncrement(),
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
