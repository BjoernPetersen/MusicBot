package com.github.bjoernpetersen.jmusicbot.playback;

import javax.annotation.Nonnull;

public interface QueueChangeListener {

  void onAdd(@Nonnull Queue.Entry entry);

  void onRemove(@Nonnull Queue.Entry entry);

  // TODO move
}
