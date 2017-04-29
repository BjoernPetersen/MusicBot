package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.playback.Playback;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class Song {

  @Nonnull
  private final PlaybackSupplier playbackSupplier;
  @Nonnull
  private final SongLoader loader;

  @Nonnull
  private final String id;
  @Nonnull
  private final String title;
  @Nonnull
  private final String description;

  @Nonnull
  private final String providerName;

  public Song(PlaybackSupplier playbackSupplier, SongLoader loader, String providerName,
    String id, String title, String description) {
    this.playbackSupplier = playbackSupplier;
    this.loader = loader;
    this.id = id;
    this.title = title;
    this.description = description;
    this.providerName = providerName;
  }

  /**
   * Schedules this song for loading. Actual loading will be done asynchronously (see {@link
   * #hasLoaded()}).
   */
  public void load() {
    loader.load(this);
  }

  /**
   * <p>Whether this specified song has been successfully loaded.</p>
   *
   * <p>This method will block until the loading is done.</p>
   *
   * @return whether loading was successful
   * @throws InterruptedException if the thread is interrupted while waiting for the song to load
   */
  public boolean hasLoaded() throws InterruptedException {
    return loader.hasLoaded(this);
  }

  /**
   * Gets a Playback object for this Song.
   * A call to this method indicates that the song will actually be played.
   *
   * @return a Playback object
   * @throws IOException if no Playback can be created
   */
  @Nonnull
  public Playback getPlayback() throws IOException {
    load();
    try {
      if (!hasLoaded()) {
        throw new IOException("Loading of song unsuccessful: " + this);
      }
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
    return playbackSupplier.supply(this);
  }

  @Nonnull
  public String getId() {
    return id;
  }

  @Nonnull
  public String getTitle() {
    return title;
  }

  @Nonnull
  public String getDescription() {
    return description;
  }

  @Nonnull
  public String getProviderName() {
    return providerName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Song song = (Song) o;

    return id.equals(song.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return "Song{"
      + "id='" + id + '\''
      + ", providerName='" + providerName + '\''
      + '}';
  }
}
