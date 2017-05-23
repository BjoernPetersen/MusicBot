package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.Song;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  public void append(@Nonnull Song song) {
    Objects.requireNonNull(song);
    if (queue.stream().noneMatch(e -> e.getSong().equals(song))) {
      queue.add(new Entry(song));
      notifyListeners(listener -> listener.onAdd(song));
    }
  }

  public void remove(@Nonnull Song song) {
    Objects.requireNonNull(song);
    for (Iterator<Entry> iterator = queue.iterator(); iterator.hasNext(); ) {
      Entry entry = iterator.next();
      if (entry.getSong().equals(song)) {
        iterator.remove();
        notifyListeners(listener -> listener.onRemove(song));
        return;
      }
    }
  }

  // TODO return entry
  @Nonnull
  public Optional<Song> pop() {
    if (queue.isEmpty()) {
      return Optional.empty();
    } else {
      Song song = queue.pop().getSong();
      notifyListeners(listener -> listener.onRemove(song));
      return Optional.of(song);
    }
  }

  public void clear() {
    queue.clear();
  }

  public Song get(int index) {
    return queue.get(index).getSong();
  }

  public List<Song> toList() {
    return queue.stream()
        .map(Entry::getSong)
        .collect(Collectors.toList());
  }

  public void addListener(@Nonnull QueueChangeListener listener) {
    listeners.add(listener);
  }

  public void removeListener(@Nonnull QueueChangeListener listener) {
    listeners.remove(listener);
  }

  private void notifyListeners(@Nonnull Consumer<QueueChangeListener> notifier) {
    for (QueueChangeListener listener : listeners) {
      notifier.accept(listener);
    }
  }

  // TODO move

  // TODO add more info, like user
  private static final class Entry {

    @Nonnull
    private final Song song;

    private Entry(@Nonnull Song song) {
      this.song = song;
    }

    @Nonnull
    public Song getSong() {
      return song;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Entry entry = (Entry) o;

      return song.equals(entry.song);
    }

    @Override
    public int hashCode() {
      return song.hashCode();
    }
  }
}
