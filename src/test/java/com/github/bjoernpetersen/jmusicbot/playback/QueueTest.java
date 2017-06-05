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
    Queue.Entry entry = mock(Queue.Entry.class);
    queue.append(entry);
    Optional<Queue.Entry> popped = queue.pop();
    assertTrue(popped.isPresent());
    assertSame(entry, popped.get());
  }

  @Test
  void appendTwice() {
    Queue queue = createValue();
    Queue.Entry entry = mock(Queue.Entry.class);
    queue.append(entry);
    queue.append(entry);
    assertEquals(1, queue.toList().size(), "Same entry appended twice");
  }

  @Test
  void fifo() {
    Queue.Entry entry1 = mock(Queue.Entry.class);
    Queue.Entry entry2 = mock(Queue.Entry.class);

    Queue queue = createValue();
    queue.append(entry1);
    queue.append(entry2);

    Optional<Queue.Entry> popped = queue.pop();
    assertTrue(popped.isPresent());
    assertSame(entry1, popped.get());

    popped = queue.pop();
    assertTrue(popped.isPresent());
    assertSame(entry2, popped.get());
  }

  @Test
  void emptyPop() {
    Optional<Queue.Entry> popped = createValue().pop();
    assertNotNull(popped);
    assertFalse(popped.isPresent());
  }

  @Test
  void clear() {
    Queue queue = createValue();
    queue.append(mock(Queue.Entry.class));
    queue.clear();
    assertFalse(queue.pop().isPresent());
  }

  @Test
  void toList() {
    Queue queue = createValue();
    Queue.Entry entry1 = mock(Queue.Entry.class);
    Queue.Entry entry2 = mock(Queue.Entry.class);
    queue.append(entry1);
    queue.append(entry2);
    assertEquals(Arrays.asList(entry1, entry2), queue.toList());
  }

  @Test
  void toListModify() {
    Queue queue = createValue();
    Queue.Entry entry = mock(Queue.Entry.class);
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
    createValue().removeListener(new QueueChangeListener() {
      @Override
      public void onAdd(@Nonnull Queue.Entry song) {
      }

      @Override
      public void onRemove(@Nonnull Queue.Entry song) {
      }
    });
  }

  @Test
  void listenerCalledAdd() {
    Queue queue = createValue();
    AtomicBoolean added = new AtomicBoolean();
    queue.addListener(new QueueChangeListener() {
      @Override
      public void onAdd(@Nonnull Queue.Entry song) {
        added.set(true);
      }

      @Override
      public void onRemove(@Nonnull Queue.Entry song) {
      }
    });
    queue.append(mock(Queue.Entry.class));
    assertTrue(added.get());
  }

  @Test
  void listenerCalledRemove() {
    Queue queue = createValue();
    queue.append(mock(Queue.Entry.class));

    AtomicBoolean removed = new AtomicBoolean();
    queue.addListener(new QueueChangeListener() {
      @Override
      public void onAdd(@Nonnull Queue.Entry song) {
      }

      @Override
      public void onRemove(@Nonnull Queue.Entry song) {
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
      public void onAdd(@Nonnull Queue.Entry song) {
        called.set(true);
      }

      @Override
      public void onRemove(@Nonnull Queue.Entry song) {
        called.set(true);
      }
    };
    queue.addListener(listener);
    queue.removeListener(listener);
    queue.append(mock(Queue.Entry.class));
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
