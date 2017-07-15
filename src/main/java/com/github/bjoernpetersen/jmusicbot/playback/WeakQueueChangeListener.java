package com.github.bjoernpetersen.jmusicbot.playback;

import java.lang.ref.WeakReference;
import java.util.Optional;
import javax.annotation.Nonnull;

public final class WeakQueueChangeListener implements QueueChangeListener {

  @Nonnull
  private final WeakReference<QueueChangeListener> wrapped;

  public WeakQueueChangeListener(@Nonnull QueueChangeListener wrapped) {
    this.wrapped = new WeakReference<>(wrapped);
  }

  @Nonnull
  private Optional<QueueChangeListener> getWrapped() {
    return Optional.ofNullable(wrapped.get());
  }

  @Override
  public void onAdd(@Nonnull QueueEntry entry) {
    getWrapped().ifPresent(wrapped -> wrapped.onAdd(entry));
  }

  @Override
  public void onRemove(@Nonnull QueueEntry entry) {
    getWrapped().ifPresent(wrapped -> wrapped.onRemove(entry));
  }
}
