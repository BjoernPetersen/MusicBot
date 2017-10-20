package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;

public final class DummyQueueChangeListener implements QueueChangeListener {

  @Override
  public void onAdd(@Nonnull QueueEntry song) {
  }

  @Override
  public void onRemove(@Nonnull QueueEntry song) {
  }

  @Override
  public void onMove(@Nonnull QueueEntry entry, int fromIndex, int toIndex) {
  }
}
