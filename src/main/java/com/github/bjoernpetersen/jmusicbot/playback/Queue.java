package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.Song;
import com.github.bjoernpetersen.jmusicbot.user.User;
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
  private final LinkedList<Entry> queue;
  @Nonnull
  private final Set<QueueChangeListener> listeners;

  Queue() {
    this.queue = new LinkedList<>();
    this.listeners = new HashSet<>();
  }

  // TODO append entry, not song
  public void append(@Nonnull Entry entry) {
    Objects.requireNonNull(entry);
    if (!queue.contains(entry)) {
      queue.add(entry);
      notifyListeners(listener -> listener.onAdd(entry));
    }
  }

  public void remove(@Nonnull Entry entry) {
    Objects.requireNonNull(entry);
    boolean removed = queue.remove(entry);
    if (removed) {
      notifyListeners(listener -> listener.onRemove(entry));
    }
  }

  @Nonnull
  public Optional<Entry> pop() {
    if (queue.isEmpty()) {
      return Optional.empty();
    } else {
      Entry entry = queue.pop();
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

  public List<Queue.Entry> toList() {
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

  public static final class Entry extends SongEntry {

    @Nonnull
    private final Song song;
    @Nonnull
    private final User user;

    public Entry(@Nonnull Song song, @Nonnull User user) {
      this.song = song;
      this.user = user;
    }

    @Nonnull
    public Song getSong() {
      return song;
    }

    @Nonnull
    public User getUser() {
      return user;
    }
  }
}
