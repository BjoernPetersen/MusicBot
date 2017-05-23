package com.github.bjoernpetersen.jmusicbot.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.github.bjoernpetersen.jmusicbot.EqualsContract;
import com.github.bjoernpetersen.jmusicbot.Song;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class QueueTest implements EqualsContract<Queue> {


  @Test
  void appendNullArg() {
    assertThrows(NullPointerException.class, () -> createValue().append(null));
  }

  @Test
  void appendValidArg() {
    Queue queue = createValue();
    Song song = mock(Song.class);
    queue.append(song);
    Optional<Song> popped = queue.pop();
    assertTrue(popped.isPresent());
    assertSame(song, popped.get());
  }

  @Test
  void appendTwice() {
    Queue queue = createValue();
    Song song = mock(Song.class);
    queue.append(song);
    queue.append(song);
    assertEquals(1, queue.toList().size(), "Same song appended twice");
  }

  @Test
  void fifo() {
    Song song1 = mock(Song.class);
    Song song2 = mock(Song.class);

    Queue queue = createValue();
    queue.append(song1);
    queue.append(song2);

    Optional<Song> popped = queue.pop();
    assertTrue(popped.isPresent());
    assertSame(song1, popped.get());

    popped = queue.pop();
    assertTrue(popped.isPresent());
    assertSame(song2, popped.get());
  }

  @Test
  void emptyPop() {
    Optional<Song> popped = createValue().pop();
    assertNotNull(popped);
    assertFalse(popped.isPresent());
  }

  @Test
  void clear() {
    Queue queue = createValue();
    queue.append(mock(Song.class));
    queue.clear();
    assertFalse(queue.pop().isPresent());
  }

  @Test
  void toList() {
    Queue queue = createValue();
    Song song1 = mock(Song.class);
    Song song2 = mock(Song.class);
    queue.append(song1);
    queue.append(song2);
    assertEquals(Arrays.asList(song1, song2), queue.toList());
  }

  @Test
  void toListModify() {
    Queue queue = createValue();
    Song song = mock(Song.class);
    queue.append(song);
    assertThrows(UnsupportedOperationException.class, () -> queue.toList().remove(song));
  }

  @Test
  void addNullListener() {
    assertThrows(NullPointerException.class, () -> createValue().addListener(null));
  }

  @Test
  void removeNullListener() {
    assertThrows(NullPointerException.class, () -> createValue().removeListener(null));
  }

  @Test
  void removeUnregisteredListener() {
    createValue().removeListener(new QueueChangeListener() {
      @Override
      public void onAdd(Song song) {
      }

      @Override
      public void onRemove(Song song) {
      }
    });
  }

  @Test
  void listenerCalledAdd() {
    Queue queue = createValue();
    AtomicBoolean added = new AtomicBoolean();
    queue.addListener(new QueueChangeListener() {
      @Override
      public void onAdd(Song song) {
        added.set(true);
      }

      @Override
      public void onRemove(Song song) {
      }
    });
    queue.append(mock(Song.class));
    assertTrue(added.get());
  }

  @Test
  void listenerCalledRemove() {
    Queue queue = createValue();
    queue.append(mock(Song.class));

    AtomicBoolean removed = new AtomicBoolean();
    queue.addListener(new QueueChangeListener() {
      @Override
      public void onAdd(Song song) {
      }

      @Override
      public void onRemove(Song song) {
        removed.set(true);
      }
    });

    queue.pop();
    assertTrue(removed.get());
  }

  @Test
  void removedNotCalledAnymore() {
    Queue queue = createValue();
    AtomicBoolean called = new AtomicBoolean();
    QueueChangeListener listener = new QueueChangeListener() {
      @Override
      public void onAdd(Song song) {
        called.set(true);
      }

      @Override
      public void onRemove(Song song) {
        called.set(true);
      }
    };
    queue.addListener(listener);
    queue.removeListener(listener);
    queue.append(mock(Song.class));
    queue.pop();
    assertFalse(called.get());
  }

  @Nonnull
  @Override
  public Queue createValue() {
    return new Queue();
  }

  @Override
  public int getEqualRelevantValueCount() {
    return 1;
  }

  @Nonnull
  @Override
  public Queue createNotEqualValue(int valueIndex) {
    return new Queue();
  }
}
