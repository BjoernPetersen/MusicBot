package com.github.bjoernpetersen.jmusicbot.playback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.github.bjoernpetersen.jmusicbot.EqualsContract;
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
    QueueEntry entry = mock(QueueEntry.class);
    queue.append(entry);
    Optional<QueueEntry> popped = queue.pop();
    assertTrue(popped.isPresent());
    assertSame(entry, popped.get());
  }

  @Test
  void appendTwice() {
    Queue queue = createValue();
    QueueEntry entry = mock(QueueEntry.class);
    queue.append(entry);
    queue.append(entry);
    assertEquals(1, queue.toList().size(), "Same entry appended twice");
  }

  @Test
  void fifo() {
    QueueEntry entry1 = mock(QueueEntry.class);
    QueueEntry entry2 = mock(QueueEntry.class);

    Queue queue = createValue();
    queue.append(entry1);
    queue.append(entry2);

    Optional<QueueEntry> popped = queue.pop();
    assertTrue(popped.isPresent());
    assertSame(entry1, popped.get());

    popped = queue.pop();
    assertTrue(popped.isPresent());
    assertSame(entry2, popped.get());
  }

  @Test
  void emptyPop() {
    Optional<QueueEntry> popped = createValue().pop();
    assertNotNull(popped);
    assertFalse(popped.isPresent());
  }

  @Test
  void clear() {
    Queue queue = createValue();
    queue.append(mock(QueueEntry.class));
    queue.clear();
    assertFalse(queue.pop().isPresent());
  }

  @Test
  void move() {
    Queue queue = createValue();
    QueueEntry entry = mock(QueueEntry.class);
    queue.append(mock(QueueEntry.class));
    queue.append(mock(QueueEntry.class));
    queue.append(entry);
    queue.append(mock(QueueEntry.class));
    queue.append(mock(QueueEntry.class));
    assertEquals(2, queue.toList().indexOf(entry));
    queue.move(entry, 1);
    assertEquals(1, queue.toList().indexOf(entry));
  }

  @Test
  void moveToStart() {
    Queue queue = createValue();
    QueueEntry entry = mock(QueueEntry.class);
    queue.append(mock(QueueEntry.class));
    queue.append(mock(QueueEntry.class));
    queue.append(entry);
    assertEquals(2, queue.toList().indexOf(entry));
    queue.move(entry, 0);
    assertEquals(0, queue.toList().indexOf(entry));
  }

  @Test
  void moveToEnd() {
    Queue queue = createValue();
    QueueEntry entry = mock(QueueEntry.class);
    queue.append(mock(QueueEntry.class));
    queue.append(entry);
    queue.append(mock(QueueEntry.class));
    assertEquals(1, queue.toList().indexOf(entry));
    queue.move(entry, 2);
    assertEquals(2, queue.toList().indexOf(entry));
  }

  @Test
  void moveToEndTooBig() {
    Queue queue = createValue();
    QueueEntry entry = mock(QueueEntry.class);
    queue.append(mock(QueueEntry.class));
    queue.append(entry);
    queue.append(mock(QueueEntry.class));
    assertEquals(1, queue.toList().indexOf(entry));
    queue.move(entry, 3);
    assertEquals(2, queue.toList().indexOf(entry));
  }

  @Test
  void moveBelowZero() {
    Queue queue = createValue();
    QueueEntry entry = mock(QueueEntry.class);
    queue.append(mock(QueueEntry.class));
    queue.append(entry);
    queue.append(mock(QueueEntry.class));
    assertThrows(IllegalArgumentException.class, () -> queue.move(entry, -1));
  }

  @Test
  void toList() {
    Queue queue = createValue();
    QueueEntry entry1 = mock(QueueEntry.class);
    QueueEntry entry2 = mock(QueueEntry.class);
    queue.append(entry1);
    queue.append(entry2);
    assertEquals(Arrays.asList(entry1, entry2), queue.toList());
  }

  @Test
  void toListModify() {
    Queue queue = createValue();
    QueueEntry entry = mock(QueueEntry.class);
    queue.append(entry);
    assertThrows(UnsupportedOperationException.class, () -> queue.toList().remove(entry));
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
    createValue().removeListener(new DummyQueueChangeListener());
  }

  @Test
  void listenerCalledAdd() {
    Queue queue = createValue();
    AtomicBoolean added = new AtomicBoolean();
    queue.addListener(new QueueChangeListener() {
      @Override
      public void onAdd(@Nonnull QueueEntry entry) {
        added.set(true);
      }

      @Override
      public void onRemove(@Nonnull QueueEntry entry) {
      }

      @Override
      public void onMove(@Nonnull QueueEntry entry, int fromIndex, int toIndex) {
      }
    });
    queue.append(mock(QueueEntry.class));
    assertTrue(added.get());
  }

  @Test
  void listenerCalledRemove() {
    Queue queue = createValue();
    queue.append(mock(QueueEntry.class));

    AtomicBoolean removed = new AtomicBoolean();
    queue.addListener(new QueueChangeListener() {
      @Override
      public void onAdd(@Nonnull QueueEntry song) {
      }

      @Override
      public void onRemove(@Nonnull QueueEntry song) {
        removed.set(true);
      }

      @Override
      public void onMove(@Nonnull QueueEntry entry, int fromIndex, int toIndex) {
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
      public void onAdd(@Nonnull QueueEntry song) {
        called.set(true);
      }

      @Override
      public void onRemove(@Nonnull QueueEntry song) {
        called.set(true);
      }

      @Override
      public void onMove(@Nonnull QueueEntry entry, int fromIndex, int toIndex) {
        called.set(true);
      }
    };
    queue.addListener(listener);
    queue.removeListener(listener);
    queue.append(mock(QueueEntry.class));
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
