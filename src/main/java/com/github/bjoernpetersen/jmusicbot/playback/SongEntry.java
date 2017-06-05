package com.github.bjoernpetersen.jmusicbot.playback;

import com.github.bjoernpetersen.jmusicbot.Song;
import com.github.bjoernpetersen.jmusicbot.user.User;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class SongEntry {

  @Nonnull
  public abstract Song getSong();

  @Nullable
  public abstract User getUser();

  @Override
  public final boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || !(o instanceof SongEntry)) {
      return false;
    }

    SongEntry entry = (SongEntry) o;

    return getSong().equals(entry.getSong());
  }

  @Override
  public final int hashCode() {
    return getSong().hashCode();
  }
}
