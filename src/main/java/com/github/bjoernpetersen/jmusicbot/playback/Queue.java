package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.Song;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

public final class Queue {

  @Nonnull
  private final LinkedList<QueueEntry> queue;
  @Nonnull
  private final Set<QueueChangeListener> listeners;

  Queue() {
    this.queue = new LinkedList<>();
    this.listeners = new HashSet<>();
  }

  // TODO append entry, not song
  public void append(@Nonnull QueueEntry entry) {
    Objects.requireNonNull(entry);
    if (!queue.contains(entry)) {
      queue.add(entry);
      notifyListeners(listener -> listener.onAdd(entry));
    }
  }

  public void remove(@Nonnull QueueEntry entry) {
    Objects.requireNonNull(entry);
    boolean removed = queue.remove(entry);
    if (removed) {
      notifyListeners(listener -> listener.onRemove(entry));
    }
  }

  @Nonnull
  public Optional<QueueEntry> pop() {
    if (queue.isEmpty()) {
      return Optional.empty();
    } else {
      QueueEntry entry = queue.pop();
      notifyListeners(listener -> listener.onRemove(entry));
      return Optional.of(entry);
    }
  }

  public void clear() {
    queue.clear();
  }

  private Song get(int index) {
    return queue.get(index).getSong();
  }

  public List<QueueEntry> toList() {
    return Collections.unmodifiableList(queue);
  }

  public void addListener(@Nonnull QueueChangeListener listener) {
    listeners.add(Objects.requireNonNull(listener));
  }

  public void removeListener(@Nonnull QueueChangeListener listener) {
    listeners.remove(Objects.requireNonNull(listener));
  }

  private void notifyListeners(@Nonnull Consumer<QueueChangeListener> notifier) {
    for (QueueChangeListener listener : listeners) {
      notifier.accept(listener);
    }
  }

  // TODO move
}
