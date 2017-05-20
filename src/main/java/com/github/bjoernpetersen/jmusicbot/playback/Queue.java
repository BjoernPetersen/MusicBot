package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.Song;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

public final class Queue {

  private final LinkedList<Entry> queue;

  Queue() {
    this.queue = new LinkedList<>();
  }

  // TODO append entry, not song
  public void append(@Nonnull Song song) {
    Objects.requireNonNull(song);
    if (queue.stream().noneMatch(e -> e.getSong().equals(song))) {
      queue.add(new Entry(song));
    }
  }

  public void remove(@Nonnull Song song) {
    Objects.requireNonNull(song);
    for (Iterator<Entry> iterator = queue.iterator(); iterator.hasNext(); ) {
      Entry entry = iterator.next();
      if (entry.getSong().equals(song)) {
        iterator.remove();
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
      return Optional.of(queue.pop().getSong());
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

  // TODO remove and move

  // TODO add more info, like user
  @ParametersAreNonnullByDefault
  private static final class Entry {

    @Nonnull
    private final Song song;

    private Entry(Song song) {
      this.song = song;
    }

    @Nonnull
    public Song getSong() {
      return song;
    }

    @Override
    public boolean equals(Object o) {
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
