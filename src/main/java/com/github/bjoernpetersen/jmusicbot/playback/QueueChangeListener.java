package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;

public interface QueueChangeListener {

  void onAdd(@Nonnull QueueEntry entry);

  void onRemove(@Nonnull QueueEntry entry);

  void onMove(@Nonnull QueueEntry entry, int fromIndex, int toIndex);
}
