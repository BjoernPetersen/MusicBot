package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.Song;
import com.github.bjoernpetersen.jmusicbot.user.User;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class SuggestedSongEntry extends SongEntry {

  @Nonnull
  private final Song song;

  SuggestedSongEntry(@Nonnull Song song) {
    this.song = song;
  }

  @Nonnull
  @Override
  public Song getSong() {
    return song;
  }

  @Nullable
  @Override
  public User getUser() {
    return null;
  }
}
