package com.github.bjoernpetersen.jmusicbot;

import com.github.bjoernpetersen.jmusicbot.playback.Playback;
import com.github.bjoernpetersen.jmusicbot.provider.Provider;
import java.io.IOException;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public final class Song {

  @Nonnull
  private final PlaybackSupplier playbackSupplier;
  @Nonnull
  private final SongLoader loader;
  @Nonnull
  private final Provider provider;

  @Nonnull
  private final String id;
  @Nonnull
  private final String title;
  @Nonnull
  private final String description;


  private Song(PlaybackSupplier playbackSupplier, SongLoader loader, Provider provider, String id,
      String title, String description) {
    this.playbackSupplier = playbackSupplier;
    this.loader = loader;
    this.provider = provider;

    this.id = id;
    this.title = title;
    this.description = description;
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
    return provider.getName();
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
        + ", providerName='" + getProviderName() + '\''
        + '}';
  }

  public static class Builder {

    private PlaybackSupplier playbackSupplier;
    private SongLoader songLoader;
    private Provider provider;

    private String id;
    private String title;
    private String description;

    // TODO album art

    @Nonnull
    public Builder playbackSupplier(@Nonnull PlaybackSupplier playbackSupplier) {
      this.playbackSupplier = Objects.requireNonNull(playbackSupplier);
      return this;
    }

    @Nonnull
    public Builder songLoader(@Nonnull SongLoader songLoader) {
      this.songLoader = Objects.requireNonNull(songLoader);
      return this;
    }

    @Nonnull
    public Builder provider(@Nonnull Provider provider) {
      this.provider = Objects.requireNonNull(provider);
      return this;
    }

    @Nonnull
    public Builder id(@Nonnull String id) {
      this.id = Objects.requireNonNull(id);
      return this;
    }

    @Nonnull
    public Builder title(@Nonnull String title) {
      this.title = Objects.requireNonNull(title);
      return this;
    }

    @Nonnull
    public Builder description(@Nonnull String description) {
      this.description = Objects.requireNonNull(description);
      return this;
    }

    @Nonnull
    public Song build() {
      if (playbackSupplier == null
          || songLoader == null
          || provider == null
          || id == null
          || title == null
          || description == null) {
        throw new IllegalStateException("Not all values specified.");
      }
      return new Song(playbackSupplier, songLoader, provider, id, title, description);
    }

  }
}
