package com.github.bjoernpetersen.jmusicbot.playback

import java.lang.ref.WeakReference
import java.util.*

class WeakQueueChangeListener(wrapped: QueueChangeListener) : QueueChangeListener {

  private val wrapped: WeakReference<QueueChangeListener> = WeakReference(wrapped)

  private fun getWrapped(): Optional<QueueChangeListener> {
    return Optional.ofNullable(wrapped.get())
  }

  override fun onAdd(entry: QueueEntry) {
    getWrapped().ifPresent { wrapped -> wrapped.onAdd(entry) }
  }

  override fun onRemove(entry: QueueEntry) {
    getWrapped().ifPresent { wrapped -> wrapped.onRemove(entry) }
  }

  override fun onMove(entry: QueueEntry, fromIndex: Int, toIndex: Int) {
    getWrapped().ifPresent { it.onMove(entry, fromIndex, toIndex) }
  }
}
