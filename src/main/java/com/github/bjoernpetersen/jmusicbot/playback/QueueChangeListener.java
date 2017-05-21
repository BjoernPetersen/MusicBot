package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.Song;

public interface QueueChangeListener {

  void onAdd(Song song);

  void onRemove(Song song);

  // TODO move
}
